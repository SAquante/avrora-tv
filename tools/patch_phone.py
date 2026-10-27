import io, re

base = 'app/src/main/java/tv/iptv/app/ui/'


def read(name):
    return io.open(base + name, encoding='utf-8').read()


def write(name, text):
    io.open(base + name, 'w', encoding='utf-8').write(text)


def patch(name, pairs):
    text = read(name)
    for old, new in pairs:
        if old not in text:
            print('MISS in', name, ':', old[:70].replace('\n', ' '))
            continue
        text = text.replace(old, new)
    write(name, text)
    print('patched', name)


# --- ChannelScreen: column count + compact padding ---
patch('ChannelScreen.kt', [
    (
        '    favoriteIds: Set<String>,\n    firstItemFocus: FocusRequester,',
        '    favoriteIds: Set<String>,\n    firstItemFocus: FocusRequester,\n    columns: Int = 2,',
    ),
    ('columns = GridCells.Fixed(2),', 'columns = GridCells.Fixed(columns),'),
    (
        'Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 22.dp))',
        'Column(\n        Modifier.fillMaxSize().padding(\n            horizontal = if (columns == 1) 14.dp else 40.dp,\n            vertical = 16.dp,\n        ),\n    )',
    ),
])

# --- RadioScreen ---
patch('RadioScreen.kt', [
    (
        '    favoriteIds: Set<String>,\n    firstItemFocus: FocusRequester,',
        '    favoriteIds: Set<String>,\n    firstItemFocus: FocusRequester,\n    columns: Int = 2,',
    ),
    ('columns = GridCells.Fixed(2),', 'columns = GridCells.Fixed(columns),'),
    (
        'Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp))',
        'Column(\n        Modifier.fillMaxSize().padding(\n            horizontal = if (columns == 1) 14.dp else 40.dp,\n            vertical = 16.dp,\n        ),\n    )',
    ),
])

# --- LanguageScreen ---
patch('LanguageScreen.kt', [
    (
        '    firstItemFocus: FocusRequester,\n    onPick: (String) -> Unit,',
        '    firstItemFocus: FocusRequester,\n    columns: Int = 2,\n    onPick: (String) -> Unit,',
    ),
    ('columns = GridCells.Fixed(2),', 'columns = GridCells.Fixed(columns),'),
    (
        'Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 26.dp))',
        'Column(\n        Modifier.fillMaxSize().padding(\n            horizontal = if (columns == 1) 14.dp else 40.dp,\n            vertical = 16.dp,\n        ),\n    )',
    ),
])

# --- CamScreen (hidden section, keep compiling) ---
patch('CamScreen.kt', [
    (
        '    favoriteIds: Set<String>,\n    firstItemFocus: FocusRequester,',
        '    favoriteIds: Set<String>,\n    firstItemFocus: FocusRequester,\n    columns: Int = 2,',
    ),
    ('columns = GridCells.Fixed(2),', 'columns = GridCells.Fixed(columns),'),
])
