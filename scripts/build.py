"""Build with Python 3, JDK 17+, Android SDK platform 36/build-tools 36.1.0.

Pinned official Maven artifacts are verified before use. The output is unsigned;
the publisher signs separately with a private key that is never part of the repo.
"""
from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
SDK = Path(os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME") or "")
JAVA = Path(os.environ.get("JAVA_HOME", ""))
if not (SDK / "platforms/android-36/android.jar").is_file():
    raise SystemExit("Set ANDROID_SDK_ROOT to an SDK containing platform android-36.")
if not (JAVA / "bin").is_dir():
    raise SystemExit("Set JAVA_HOME to JDK 17 or later.")
BT = SDK / "build-tools/36.1.0"
ANDROID = SDK / "platforms/android-36/android.jar"
BUILD = ROOT / "build/public"
BUILD.mkdir(parents=True, exist_ok=True)
DEPS = BUILD / "deps"
DEPS.mkdir(exist_ok=True)
ARTIFACTS = {
    "api": "423484a6e1807e7a423c4b88fcd8176d104318259d91791877fed88fe91479d0",
    "service": "a665e4af4638924047f5d5cb610553e15bc2c0ea2ce64a27afb6dee257ff014c",
    "interface": "4e0b76d6050be1d3dc0d35525180ad7d9b60a47fd8d8fe1d18c2bdc840be9ddb",
}

def tool(folder, name, batch=False):
    return str(folder / (name + (".bat" if batch else ".exe") if os.name == "nt" else name))

def run(*args):
    subprocess.run([str(arg) for arg in args], check=True)

for artifact, digest in ARTIFACTS.items():
    aar = DEPS / f"{artifact}-102.0.0.aar"
    if not aar.exists():
        url = f"https://repo.maven.apache.org/maven2/io/github/libxposed/{artifact}/102.0.0/{artifact}-102.0.0.aar"
        print(f"Downloading {artifact} 102.0.0", flush=True)
        urllib.request.urlretrieve(url, aar)
    if hashlib.sha256(aar.read_bytes()).hexdigest() != digest:
        raise SystemExit(f"SHA-256 mismatch: {aar}")
    with zipfile.ZipFile(aar) as archive:
        (DEPS / f"{artifact}.jar").write_bytes(archive.read("classes.jar"))

# Use only this invocation's class files so removed/renamed classes cannot leak in.
classes = BUILD / "classes"
if classes.exists():
    resolved = classes.resolve()
    if resolved.parent != BUILD.resolve():
        raise SystemExit("Unexpected classes directory")
    shutil.rmtree(resolved)
classes.mkdir()
dex = BUILD / "dex"
dex.mkdir(exist_ok=True)
resources = BUILD / "resources.zip"
run(tool(BT, "aapt2"), "compile", "--dir", "app/src/main/res", "-o", resources)
run(tool(BT, "aapt2"), "link", "-o", BUILD / "base.apk", "-I", ANDROID,
    "--manifest", "app/src/main/AndroidManifest.xml", resources)
classpath = os.pathsep.join(str(p) for p in [ANDROID, DEPS / "api.jar", DEPS / "service.jar"])
run(tool(JAVA / "bin", "javac"), "-encoding", "UTF-8", "--release", "17",
    "-classpath", classpath, "-d", classes,
    *sorted(ROOT.glob("app/src/main/java/**/*.java")))
run(tool(JAVA / "bin", "jar"), "--create", "--file", BUILD / "app.jar", "-C", classes, ".")
run(tool(BT, "d8", batch=True), "--release", "--min-api", "29", "--lib", ANDROID,
    "--classpath", DEPS / "api.jar", "--output", dex, BUILD / "app.jar",
    DEPS / "service.jar", DEPS / "interface.jar")
with zipfile.ZipFile(BUILD / "base.apk") as base, zipfile.ZipFile(
    BUILD / "unsigned.apk", "w", zipfile.ZIP_DEFLATED
) as output:
    for info in base.infolist():
        output.writestr(info, base.read(info.filename))
    output.write(dex / "classes.dex", "classes.dex")
    for file in sorted(Path("app/src/main/resources").rglob("*")):
        if file.is_file():
            output.write(file, file.relative_to("app/src/main/resources").as_posix())
dist = ROOT / "dist"
dist.mkdir(exist_ok=True)
run(tool(BT, "zipalign"), "-f", "4", BUILD / "unsigned.apk", dist / "NoWait10s-0.1.0-unsigned.apk")
print("Built dist/NoWait10s-0.1.0-unsigned.apk")
