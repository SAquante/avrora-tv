"""Try several InnerTube clients and report which ones expose hlsManifestUrl for a live video."""
import json
import urllib.request

VIDEO = 'jfKfPfyJRdk'  # long-running live stream
URL = 'https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8'

CLIENTS = [
    ('WEB', {'clientName': 'WEB', 'clientVersion': '2.20240726.00.00'}),
    ('MWEB', {'clientName': 'MWEB', 'clientVersion': '2.20240726.01.00'}),
    ('IOS', {'clientName': 'IOS', 'clientVersion': '19.29.1',
             'deviceModel': 'iPhone16,2', 'osName': 'iPhone', 'osVersion': '17.5.1.21F90'}),
    ('ANDROID', {'clientName': 'ANDROID', 'clientVersion': '19.29.37', 'androidSdkVersion': 30}),
    ('ANDROID_VR', {'clientName': 'ANDROID_VR', 'clientVersion': '1.60.19',
                    'deviceModel': 'Quest 3', 'androidSdkVersion': 32}),
    ('TVHTML5_SIMPLY_EMBEDDED_PLAYER', {'clientName': 'TVHTML5_SIMPLY_EMBEDDED_PLAYER',
                                        'clientVersion': '2.0'}),
]


def main():
    for name, client in CLIENTS:
        body = {
            'context': {'client': dict(client, hl='en', gl='US')},
            'videoId': VIDEO,
            'contentCheckOk': True,
            'racyCheckOk': True,
        }
        req = urllib.request.Request(
            URL,
            data=json.dumps(body).encode(),
            headers={
                'Content-Type': 'application/json',
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0.0.0',
                'X-YouTube-Client-Name': name,
                'X-YouTube-Client-Version': client['clientVersion'],
                'Origin': 'https://www.youtube.com',
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=25) as resp:
                data = json.load(resp)
            status = data.get('playabilityStatus', {}).get('status')
            hls = data.get('streamingData', {}).get('hlsManifestUrl')
            print(f'{name:34} status={status} hls={bool(hls)}')
            if hls:
                print('   ', hls[:110])
        except Exception as exc:  # noqa: BLE001 - just report and continue
            print(f'{name:34} failed: {exc}')


if __name__ == '__main__':
    main()
