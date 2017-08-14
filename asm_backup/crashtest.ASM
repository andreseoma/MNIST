format PE64
;use64

include 'win64ax.inc'

s0=28*28
s1=100
s2=10

section '.text' code readable executable
sub rsp,8 ;align stack to multiple of 16

invoke QueryPerformanceFrequency, freq
invoke printf,"%d ",rax
jmp p
invoke QueryPerformanceCounter,start

invoke QueryPerformanceCounter,ended
fild qword [freq]
fild qword [ended]
fild qword [start]
fsubp st1,st0
fdiv st0,st1
fstp qword [temp]
finit
invoke printf,"%.6f ",[temp]

p: invoke printf,"END",
invoke Sleep,-1
invoke ExitProcess,0

section '.data' data readable writeable
dat dq 0
dattemp dq 0
labels dq 0
labelstemp dq 0
picnum dq 0
w1 dq 0
w2 dq 0
b1 dq 0
b2 dq 0
v1 dq 0
v2 dq 0
gw1 dq 0
gsw1 dq 0
gw2 dq 0
gsw2 dq 0
gb1 dq 0
gsb1 dq 0
gb2 dq 0
gsb2 dq 0

f1n db 'D:\Java\Projekt\train-images.idx3-ubyte',0
f1s dq 0
f2n db 'D:\Java\Projekt\train-labels.idx1-ubyte',0
f2s dq 0
f3n db 'Weightsasm.txt',0
align 8
f1 dq 0
f2 dq 0
f3 dq 0
temp dq 0
temp2 dq 0
powtwo dq -63
maxintensity dq 255
one dq 1.0
allones dq 2
freq dq 0
start dq 0
ended dq 0
stocount dq 10f
learningrate dq 0.1f

ws1 dq s0 ;w1 scale
ws1d dq 0.2f  ;w1 scale from dispersion of first (input) layer
ws2 dq s1 ;w2 scale

msg db "hello",0

section ".idata" import data readable

library kernel32,'KERNEL32.DLL',\
	user32,'USER32.DLL',\
	msvcrt,'msvcrt.dll'
include 'API\USER32.INC'
include 'API\KERNEL32.INC'

import msvcrt,printf,'printf',\
	      rand_s,'rand_s'