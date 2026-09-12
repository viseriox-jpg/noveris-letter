#!/usr/bin/env python3
import pathlib
import shutil
import sys
import zipfile

JAR = pathlib.Path(sys.argv[1])
ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources/assets/noveris_letter"

ASSETS = {
    "assets/cropcritters/geo/entity/melon_critter.geo.json": RES / "geo/entity/melon_critter.geo.json",
    "assets/cropcritters/geo/entity/carrot_critter.geo.json": RES / "geo/entity/carrot_critter.geo.json",
    "assets/cropcritters/geo/entity/wheat_critter.geo.json": RES / "geo/entity/wheat_critter.geo.json",
    "assets/cropcritters/geo/entity/pumpkin_critter.geo.json": RES / "geo/entity/pumpkin_critter.geo.json",
    "assets/cropcritters/geo/entity/potato_critter.geo.json": RES / "geo/entity/potato_critter.geo.json",
    "assets/cropcritters/textures/entity/critters/melon_critter.png": RES / "textures/entity/critters/melon_critter.png",
    "assets/cropcritters/textures/entity/critters/carrot_critter.png": RES / "textures/entity/critters/carrot_critter.png",
    "assets/cropcritters/textures/entity/critters/wheat_critter.png": RES / "textures/entity/critters/wheat_critter.png",
    "assets/cropcritters/textures/entity/critters/pumpkin_critter.png": RES / "textures/entity/critters/pumpkin_critter.png",
    "assets/cropcritters/textures/entity/critters/potato_critter.png": RES / "textures/entity/critters/potato_critter.png",
    "assets/cropcritters/animations/entity/basic_critter.animation.json": RES / "animations/entity/basic_critter.animation.json",
    "assets/cropcritters/animations/entity/pumpkin_critter.animation.json": RES / "animations/entity/pumpkin_critter.animation.json",
}

with zipfile.ZipFile(JAR) as z:
    for source, target in ASSETS.items():
        if source not in z.namelist():
            raise SystemExit(f"Missing required Crop Critters asset: {source}")
        target.parent.mkdir(parents=True, exist_ok=True)
        with z.open(source) as src, target.open("wb") as dst:
            shutil.copyfileobj(src, dst)

print(f"Imported {len(ASSETS)} exact Crop Critters courier assets from {JAR.name}")
