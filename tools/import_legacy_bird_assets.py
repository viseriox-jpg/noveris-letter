#!/usr/bin/env python3
import pathlib, sys, zipfile

src = pathlib.Path(sys.argv[1])
out = pathlib.Path('src/main/resources/assets/noveris_letter')
files = {
    'assets/creatures/geo/entity/sparrow/sparrowfly.geo.json': out / 'geo/entity/sparrow/sparrowfly.geo.json',
    'assets/creatures/animations/animation.sparrow.fly.json': out / 'animations/animation.sparrow.fly.json',
    'assets/creatures/textures/entity/sparrow/sparrow1ffly.png': out / 'textures/entity/courier/sparrowfly.png',
}
with zipfile.ZipFile(src) as jar:
    for source, target in files.items():
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(jar.read(source))
print(f'Imported {len(files)} Sparrow courier assets from {src}')
