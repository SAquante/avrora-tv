import io, os, shutil

manifest_path = 'app/src/main/AndroidManifest.xml'
text = io.open(manifest_path, encoding='utf-8').read()
text = text.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@drawable/ic_launcher"')
text = text.replace('\n        android:roundIcon="@mipmap/ic_launcher"', '')
io.open(manifest_path, 'w', encoding='utf-8').write(text)

adaptive_dir = 'app/src/main/res/mipmap-anydpi-v26'
if os.path.isdir(adaptive_dir):
    shutil.rmtree(adaptive_dir)
    print('removed adaptive dir')

print([line.strip() for line in text.splitlines() if 'android:icon' in line or 'banner' in line])
