import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()

# the centre button already shows the state; the extra pause caption only clutters the video
start = text.find('            if (paused && !buffering) {')
if start == -1:
    print('MISS pause caption')
else:
    end = text.find('            }\n', text.find('padding(horizontal = 18.dp, vertical = 10.dp),', start))
    end = text.find('\n            }\n', start)
    # remove the whole if-block up to its closing brace
    close = text.find('\n            }\n', text.find('Text(', start))
    block_end = text.find('\n            }\n', close + 1) if close != -1 else -1
    if block_end != -1:
        text = text[:start] + text[block_end + len('\n            }\n'):]
        print('removed pause caption')
    else:
        print('MISS block end')

io.open(path, 'w', encoding='utf-8').write(text)
