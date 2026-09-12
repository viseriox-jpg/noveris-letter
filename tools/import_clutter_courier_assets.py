#!/usr/bin/env python3
import pathlib, struct, sys, zipfile

JAR = pathlib.Path(sys.argv[1])
ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/noveris_letter'
PRE = ROOT / 'src/main/precompiled'

M = {
    'net/emilsg/clutterbestiary/entity/client/model/BoopletModel':'dev/noveris/letter/courier/precompiled/BoopletModel',
    'net/emilsg/clutterbestiary/entity/client/model/CapybaraModel':'dev/noveris/letter/courier/precompiled/CapybaraModel',
    'net/emilsg/clutterbestiary/entity/client/model/CoatiModel':'dev/noveris/letter/courier/precompiled/CoatiModel',
    'net/emilsg/clutterbestiary/entity/client/model/MossbloomModel':'dev/noveris/letter/courier/precompiled/MossbloomModel',
    'net/emilsg/clutterbestiary/entity/client/model/RedPandaModel':'dev/noveris/letter/courier/precompiled/RedPandaModel',
    'net/emilsg/clutterbestiary/entity/client/model/parent/BestiaryModel':'dev/noveris/letter/courier/precompiled/BestiaryModel',
    'net/emilsg/clutterbestiary/entity/client/model/parent/ParentTameableModel':'dev/noveris/letter/courier/precompiled/ParentTameableModel',
    'net/emilsg/clutterbestiary/entity/client/animation/BoopletEntityAnimations':'dev/noveris/letter/courier/precompiled/BoopletEntityAnimations',
    'net/emilsg/clutterbestiary/entity/client/animation/CapybaraEntityAnimations':'dev/noveris/letter/courier/precompiled/CapybaraEntityAnimations',
    'net/emilsg/clutterbestiary/entity/client/animation/CoatiAnimations':'dev/noveris/letter/courier/precompiled/CoatiAnimations',
    'net/emilsg/clutterbestiary/entity/client/animation/MossbloomEntityAnimations':'dev/noveris/letter/courier/precompiled/MossbloomEntityAnimations',
    'net/emilsg/clutterbestiary/entity/client/animation/RedPandaAnimations':'dev/noveris/letter/courier/precompiled/RedPandaAnimations',
    'net/emilsg/clutterbestiary/entity/variants/MossbloomVariant':'dev/noveris/letter/courier/precompiled/MossbloomVariant',
    'net/emilsg/clutterbestiary/entity/custom/BoopletEntity':'dev/noveris/letter/courier/CourierBoopletEntity',
    'net/emilsg/clutterbestiary/entity/custom/CapybaraEntity':'dev/noveris/letter/courier/CourierCapybaraEntity',
    'net/emilsg/clutterbestiary/entity/custom/CoatiEntity':'dev/noveris/letter/courier/CourierCoatiEntity',
    'net/emilsg/clutterbestiary/entity/custom/MossbloomEntity':'dev/noveris/letter/courier/CourierMossbloomEntity',
    'net/emilsg/clutterbestiary/entity/custom/RedPandaEntity':'dev/noveris/letter/courier/CourierRedPandaEntity',
    'net/emilsg/clutterbestiary/entity/custom/parent/ParentAnimalEntity':'dev/noveris/letter/courier/CourierBirdEntity',
    'net/emilsg/clutterbestiary/entity/custom/parent/ParentTameableEntity':'dev/noveris/letter/courier/CourierBirdEntity',
    'clutterbestiary':'noveris_letter',
}

def transform(data):
    cp_count = struct.unpack('>H', data[8:10])[0]
    pos = 10
    pieces = []
    i = 1
    while i < cp_count:
        tag = data[pos]
        start = pos
        pos += 1
        if tag == 1:
            ln = struct.unpack('>H', data[pos:pos+2])[0]
            pos += 2
            raw = data[pos:pos+ln]
            pos += ln
            text = raw.decode('utf-8', errors='surrogatepass')
            for a, b in sorted(M.items(), key=lambda x: -len(x[0])):
                text = text.replace(a, b)
            raw = text.encode('utf-8')
            pieces.append(bytes([1]) + struct.pack('>H', len(raw)) + raw)
        else:
            if tag in (3, 4): end = pos + 4
            elif tag in (5, 6): end = pos + 8
            elif tag in (7, 8, 16, 19, 20): end = pos + 2
            elif tag in (9, 10, 11, 12, 17, 18): end = pos + 4
            elif tag == 15: end = pos + 3
            else: raise ValueError(f'Unknown constant-pool tag {tag} at {i}')
            pieces.append(data[start:end])
            pos = end
            if tag in (5, 6):
                pieces.append(b'')
                i += 1
        i += 1
    return data[:8] + data[8:10] + b''.join(pieces) + data[pos:]

def copy_asset(z, source, target):
    data = z.read(source)
    if not data:
        raise RuntimeError(f'empty asset: {source}')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)

with zipfile.ZipFile(JAR) as z:
    assets = {
        'assets/clutterbestiary/textures/entity/booplet/booplet.png': RES/'textures/entity/courier/booplet.png',
        'assets/clutterbestiary/textures/entity/capybara/capybara.png': RES/'textures/entity/courier/capybara.png',
        'assets/clutterbestiary/textures/entity/coati/jungle_coati.png': RES/'textures/entity/courier/coati.png',
        'assets/clutterbestiary/textures/entity/mossbloom/horned_mossbloom.png': RES/'textures/entity/courier/mossbloom.png',
        'assets/clutterbestiary/textures/entity/red_panda/full_red_panda.png': RES/'textures/entity/courier/red_panda.png',
    }
    for source, target in assets.items():
        copy_asset(z, source, target)

    classes = [
        'net/emilsg/clutterbestiary/entity/client/model/BoopletModel.class',
        'net/emilsg/clutterbestiary/entity/client/model/CapybaraModel.class',
        'net/emilsg/clutterbestiary/entity/client/model/CoatiModel.class',
        'net/emilsg/clutterbestiary/entity/client/model/MossbloomModel.class',
        'net/emilsg/clutterbestiary/entity/client/model/RedPandaModel.class',
        'net/emilsg/clutterbestiary/entity/client/model/parent/BestiaryModel.class',
        'net/emilsg/clutterbestiary/entity/client/model/parent/ParentTameableModel.class',
        'net/emilsg/clutterbestiary/entity/client/animation/BoopletEntityAnimations.class',
        'net/emilsg/clutterbestiary/entity/client/animation/CapybaraEntityAnimations.class',
        'net/emilsg/clutterbestiary/entity/client/animation/CoatiAnimations.class',
        'net/emilsg/clutterbestiary/entity/client/animation/MossbloomEntityAnimations.class',
        'net/emilsg/clutterbestiary/entity/client/animation/RedPandaAnimations.class',
        'net/emilsg/clutterbestiary/entity/variants/MossbloomVariant.class',
    ]
    for source in classes:
        target_name = M[source[:-6]] + '.class'
        target = PRE / target_name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(transform(z.read(source)))

print(f'Imported Clutter courier models, animations and textures from {JAR}')
