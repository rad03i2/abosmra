"""Deterministic original alert recordings; no Apple audio files are bundled.

The classic presets evoke familiar phone alert patterns. Run this script when
changing the instruments; generated PCM WAVs are versioned for offline use.
"""
from pathlib import Path
import math
import wave
import struct

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
# (frequency Hz, duration ms, gain, metallic), gap ms
PRESETS = {
 'cash_register': ([(1318.51,70,.34,True),(1760,95,.38,True),(2637.02,185,.40,True)],24),
 'coin_cascade': ([(2093,65,.36,True),(2637.02,70,.38,True),(2349.32,75,.36,True),(3135.96,150,.40,True)],18),
 'pos_premium': ([(783.99,105,.31,False),(1174.66,125,.34,False),(1568,215,.36,False)],30),
 'cash_ping': ([(1568,80,.34,True),(2349.32,155,.38,True)],22),
 'soft_bell': ([(987.77,120,.30,False),(1479.98,220,.32,False)],34),
 'double_chime': ([(1046.50,115,.33,False),(1568,115,.35,False),(2093,170,.36,False)],45),
 'classic_tri_tone': ([(1568,115,.32,False),(2093,115,.34,False),(2637.02,300,.34,False)],45),
 'classic_note': ([(1318.51,570,.35,False)],0),
 'classic_glass': ([(2093,135,.26,True),(3135.96,520,.30,True)],18),
 'classic_chime': ([(1046.50,180,.30,True),(1568,220,.31,True),(2093,470,.30,True)],60),
 'classic_complete': ([(783.99,120,.30,False),(987.77,120,.30,False),(1174.66,135,.32,False),(1568,420,.32,False)],25),
 'classic_rebound': ([(1174.66,100,.28,False),(1568,145,.32,False),(1318.51,180,.31,False),(1568,330,.28,False)],35),
}

def samples(notes, gap, legacy=False):
 result=[]
 for index,(frequency,ms,gain,metallic) in enumerate(notes):
  length=round(RATE*ms/1000)
  for i in range(length):
   progress=i/max(1,length)
   attack=min(1,progress/(.07 if legacy else .035))
   decay=1.0 if legacy else math.exp(-3.8*progress)
   release=min(1,(1-progress)/(.82 if legacy else .18))
   t=i/RATE
   sound=math.sin(2*math.pi*frequency*t)
   harmonics=(((2.71,.32),(4.19,.14)) if metallic else ((2,.20),(3,.08))) if legacy else (((2.71,.26),(4.19,.10)) if metallic else ((2,.16),(3,.05)))
   for multiplier,weight in harmonics:
    sound+=weight*math.sin(2*math.pi*frequency*multiplier*t)
   result.append(max(-32768,min(32767,round(sound*attack*decay*release*gain*32767))))
  if index<len(notes)-1: result.extend([0]*round(RATE*gap/1000))
 result.extend([0]*round(RATE*.07))
 return result

if __name__=='__main__':
 ROOT.mkdir(parents=True,exist_ok=True)
 for name,(notes,gap) in PRESETS.items():
  pcm=samples(notes,gap,legacy=not name.startswith("classic_"))
  with wave.open(str(ROOT/(name+'.wav')),'wb') as output:
   output.setparams((1,2,RATE,0,'NONE','not compressed'))
   output.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
 print(f'Generated {len(PRESETS)} offline alert recordings.')
