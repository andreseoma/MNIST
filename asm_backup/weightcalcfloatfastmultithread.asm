format PE64
;use64

include 'win64ax.inc'

s0=28*28
s1=16*10
s2=10
THREADCOUNT=2

section '.text' code readable executable
sub rsp,8 ;align stack to multiple of 16

;||||||||||||||START MACROS||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
macro ThreadBarrier register{
		mov register,1
		lock xadd [lock1],register
		cmp register,THREADCOUNT-1
		jne @f
		mov [lock2],0

		@@: cmp [lock2],THREADCOUNT
		je @b

		mov register,1
		lock xadd [lock2],register
		cmp register,THREADCOUNT-1
		jne @f
		mov [lock1],0

		@@: cmp [lock1],THREADCOUNT
		je @b
}

macro loaddata dataset{
      local endit
      if dataset eq trainingdata
	 mov rax,[dattraining]
	 test rax,rax
	 jz @f
	 mov [dat],rax
	 mov rax,[picnumtraining]
	 mov [picnum],rax
	 mov rax,[labelstraining]
	 mov [labels],rax
	 jmp endit
      else
	 mov rax,[dattest]
	 test rax,rax
	 jz @f
	 mov [dat],rax
	 mov rax,[picnumtest]
	 mov [picnum],rax
	 mov rax,[labelstest]
	 mov [labels],rax
	 jmp endit
      end if
@@:
stmxcsr dword [temp]
mov eax,dword [temp]
or eax, 0x8040	;flush to zero and zero denormal numbers
mov dword [temp],eax
ldmxcsr dword [temp]

	if dataset eq trainingdata
invoke CreateFile, f1n,GENERIC_READ,FILE_SHARE_WRITE,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0
mov [f1],rax
invoke GetFileSize,[f1],0
mov [f1s],rax
invoke VirtualAlloc, 0,[f1s],MEM_COMMIT+MEM_RESERVE,0x04
mov [dattemp],rax
invoke ReadFile, [f1],[dattemp],[f1s],temp,0
invoke CloseHandle,[f1]

invoke CreateFile, f2n,GENERIC_READ,FILE_SHARE_WRITE,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0
mov [f2],rax
invoke GetFileSize,[f2],0
mov [f2s],rax
invoke VirtualAlloc, 0,[f2s],MEM_COMMIT+MEM_RESERVE,0x04
mov [labelstemp],rax
invoke ReadFile, [f2],[labelstemp],[f2s],temp,0
invoke CloseHandle,[f2]
mov rcx,[f2s]
sub rcx,8
mov [picnumtraining], rcx
mov [picnum],rcx
       else
invoke CreateFile, f4n,GENERIC_READ,FILE_SHARE_WRITE,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0
mov [f4],rax
invoke GetFileSize,[f4],0
mov [f4s],rax
invoke VirtualAlloc, 0,[f4s],MEM_COMMIT+MEM_RESERVE,0x04
mov [dattemp],rax
invoke ReadFile, [f4],[dattemp],[f4s],temp,0
invoke CloseHandle,[f4]

invoke CreateFile, f5n,GENERIC_READ,FILE_SHARE_WRITE,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0
mov [f5],rax
invoke GetFileSize,[f5],0
mov [f5s],rax
invoke VirtualAlloc, 0,[f5s],MEM_COMMIT+MEM_RESERVE,0x04
mov [labelstemp],rax
invoke ReadFile, [f5],[labelstemp],[f5s],temp,0
invoke CloseHandle,[f5]
mov rcx,[f5s]
sub rcx,8
mov [picnumtest], rcx
mov [picnum],rcx
       end if


;align labels data to a new array
       if dataset eq trainingdata
invoke VirtualAlloc, 0,[f2s],MEM_COMMIT+MEM_RESERVE,0x04
mov [labelstraining],rax
mov [labels], rax
       else
invoke VirtualAlloc, 0,[f4s],MEM_COMMIT+MEM_RESERVE,0x04
mov [labelstest],rax
mov [labels],rax
       end if
mov rdi,[labels]
mov rsi,[labelstemp]
add rsi,8
mov rcx,[picnum]
rep movsb
invoke VirtualFree,[labelstemp],0,MEM_RELEASE

;load bytes range 0-255 to doubles range 0-1 to dat
mov rax,[picnum]
imul rax, s0*4
invoke VirtualAlloc, 0,rax,MEM_COMMIT+MEM_RESERVE,0x04
     if dataset eq trainingdata
mov [dattraining],rax
    else
mov [dattest],rax
    end if
mov [dat],rax
mov rsi,[dattemp]
add rsi,16
mov rdi,[dat]
mov rbx,[picnum]
imul rbx,s0
fild qword [maxintensity]
xor rax,rax
mov [temp],rax
@@: mov al,[rsi]
mov byte [temp],al
fild word [temp]
fdiv st0,st1
fstp dword [rdi]
inc rsi
add rdi,4
dec rbx
jnz @b
finit
invoke VirtualFree,[dattemp],0,MEM_RELEASE
endit:
}

macro allocweights{
invoke VirtualAlloc, 0,s1*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [v1],rax
invoke VirtualAlloc, 0,(s2+7)*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [v2],rax
invoke VirtualAlloc, 0,s1*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [b1],rax
invoke VirtualAlloc, 0,(s2+7)*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [b2],rax
invoke VirtualAlloc, 0,s1*s0*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [w1],rax
invoke VirtualAlloc, 0,s2*s1*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [w2],rax
invoke VirtualAlloc, 0,s1*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [gb1],rax
invoke VirtualAlloc, 0,s1*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [gsb1],rax
invoke VirtualAlloc, 0,(s2+7)*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [gb2],rax
invoke VirtualAlloc, 0,(s2+7)*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [gsb2],rax
invoke VirtualAlloc, 0,s1*s0*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [gw1],rax
invoke VirtualAlloc, 0,s1*s0*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [gsw1],rax
invoke VirtualAlloc, 0,s2*s1*4*10,MEM_COMMIT+MEM_RESERVE,0x04
mov [gw2],rax
invoke VirtualAlloc, 0,s2*s1*4,MEM_COMMIT+MEM_RESERVE,0x04
mov [gsw2],rax
}

;MACROS{{{{{{{{{{{{{{{{{{{

macro randomizeweights{  ;random rbx doubles gaussian, std 1, mean 0 to rdi
      local rand
finit
rand:
invoke rand_s,temp
invoke rand_s,temp+4
and byte [temp+7],0x7f
fild qword [powtwo]
fild qword [temp]
fscale
fxch st1
invoke rand_s,temp
invoke rand_s,temp+4
and byte [temp+7],0x7f
fild qword [temp]
fscale
fxch
fstp st0;now two uniform random variables in st0 and st1
fld1
fadd st0,st0
fchs
fld st1
fyl2x
fldl2e
fdivp st1,st0
fsqrt
fldpi
fadd st0,st0
fmul st0,st3
fld st0 ;2pix2,2pix2,sqrt(-ln(2x1),x1,x2
fcos
fmul st0,st2
fstp dword [rdi]
add rdi,4
dec rbx
jz @f
fsin
fmulp st1,st0
fstp dword [rdi]
fstp st0
fstp st0
add rdi,4
dec rbx
jz @f
jmp rand
@@:
}

;normalize weights
macro normalize{
fild qword [ws1]
fld qword [ws1d]
fmulp st1,st0
fsqrt
mov rcx,s0*s1
mov rdi,[w1]
@@:fld dword [rdi]
fdiv st0,st1
fstp dword [rdi]
add rdi,4
dec rcx
jnz @b
mov rcx,s1
mov rdi,[b1]
@@:fld dword [rdi]
fdiv st0,st1
fstp dword [rdi]
add rdi,4
dec rcx
jnz @b
fstp st0

fild qword [ws2]
fsqrt
mov rcx,s1*s2
mov rdi,[w2]
@@:fld dword [rdi]
fdiv st0,st1
fstp dword [rdi]
add rdi,4
dec rcx
jnz @b
mov rcx,s2
mov rdi,[b2]
@@:fld dword [rdi]
fdiv st0,st1
fstp dword [rdi]
add rdi,4
dec rcx
jnz @b
fstp st0
}


;random to rdx and r10, offset to r13
macro randompic{
invoke rand_s,temp
mov eax,dword [temp]
xor rdx,rdx
mov rbx,[picnum]
sub rbx,10
div ebx
mov r13d,edx
mov r10d,edx
imul r13,s0*4
mov [curpic],r10
mov [curoffset],r13
}

macro loadpicnum numero{
      mov r13,numero
      mov r10,numero
      imul r13,s0*4
      mov rbp,[labels]
      movzx rbp,byte [rbp+numero]
      mov [curpic],r10
      mov [curoffset],r13
}

;feedforward 10 pictures at a time
macro feedf ThreadCount{
;from first layer to the second, curOffset is in r13
      local feed1, feed12, feed2
mov rax,[w1]
mov rdi,[v1]
mov rdx,[b1]
mov rbx,s1/2
mov r8,[dat]
mov r13,[curoffset]
add r8,r13
    feed1:
vzeroall
mov rcx,s0/8/2
mov r9,r8
@@: vmovaps ymm10,[rax+ThreadCount*s0*s1/2*4]

rept 10 n:0 \{
	vmovaps ymm11,[r9+s0*4*n]
	vmulps ymm11,ymm11,ymm10
	vaddps ymm\#n,ymm\#n,ymm11
\}
    add rax,4*8
    add r9,4*8
    dec rcx
    jnz @b

    vxorps ymm15,ymm15,ymm15
    vmovss xmm14,dword [rdx+ThreadCount*s1/2*4]

rept 10 n:0
\{
       vhaddps ymm10,ymm\#n,ymm\#n
       vhaddps ymm10,ymm10,ymm10
       vperm2f128 ymm11,ymm10,ymm10,0x11
       vaddps ymm10,ymm10,ymm11
       vaddss xmm10,xmm10,xmm14
       vmovss [rdi+s1*4*n+ThreadCount*s1/2*4],xmm10
\}
	add rax,4*s0/2
	add rdi,4
	add rdx,4
	dec rbx
	jnz feed1

mov rax,[w1]
mov rdi,[v1]
mov rdx,[b1]
mov rbx,s1/2
add r8,4*s0/2
    feed12:
vzeroall
mov rcx,s0/8/2
add rax,4*s0/2
mov r9,r8
@@: vmovaps ymm10,[rax+ThreadCount*s0*s1/2*4]

rept 10 n:0 \{
	vmovaps ymm11,[r9+s0*4*n]
	vmulps ymm11,ymm11,ymm10
	vaddps ymm\#n,ymm\#n,ymm11
\}
    add rax,4*8
    add r9,4*8
    dec rcx
    jnz @b

    vxorps ymm15,ymm15,ymm15
    vmovss xmm14,dword [rdx+ThreadCount*s1/2*4]

rept 10 n:0
\{
       vhaddps ymm10,ymm\#n,ymm\#n
       vhaddps ymm10,ymm10,ymm10
       vperm2f128 ymm11,ymm10,ymm10,0x11
       vaddps ymm10,ymm10,ymm11
       vaddss xmm10,xmm10,[rdi+s1*4*n+ThreadCount*s1/2*4]
       vcmpnleps ymm11,ymm10,ymm15
       vandps ymm10,ymm10,ymm11
       vmovss [rdi+s1*4*n+ThreadCount*s1/2*4],xmm10
\}

	add rdi,4
	add rdx,4
	dec rbx
	jnz feed12

ThreadBarrier rax

;from the second layer to the third
mov rax,[w2]
mov rdi,[v2]
mov rdx,[b2]
mov rbx,s2/2
mov r8,[v1]
    feed2:
vzeroall
mov rcx,s1/8
mov r9,r8
@@: vmovaps ymm10,[rax+ThreadCount*s1*s2/2*4]

rept 10 n:0 \{
   rept 1 m:11+(n mod 5) \\{
	vmovaps ymm\\#m,[r9+s1*4*n]
	vmulps ymm\\#m,ymm\\#m,ymm10
	vaddps ymm\#n,ymm\#n,ymm\\#m
   \\}
\}
    add rax,4*8
    add r9,4*8
    dec rcx
    jnz @b

vxorps ymm15,ymm15,ymm15
vmovss xmm14,[rdx+ThreadCount*s2/2*4]

rept 10 n:0
\{
       vhaddps ymm10,ymm\#n,ymm\#n
       vhaddps ymm10,ymm10,ymm10
       vperm2f128 ymm11,ymm10,ymm10,0x11
       vaddps ymm10,ymm10,ymm11
       vaddss xmm10,xmm10,xmm14
       vcmpnleps ymm11,ymm10,ymm15
       vandps ymm10,ymm10,ymm11
       vmovss [rdi+s2*4*n+ThreadCount*s2/2*4],xmm10
\}

	add rdi,4
	add rdx,4
	dec rbx
	jnz feed2
}

;backpropagation 10 pictures at a time
macro backprop ThreadCount{
local gw1loop, gw2loop, gw22loop
      mov r10,[curpic]
    if ThreadCount eq 0
      mov rdx,[gb2]
      mov rbx,[v2]

	;mov v2 to gb2
      mov rcx,((s2*10+7)/8)
      @@:
      vmovaps ymm0,[rbx]
      vmovaps [rdx],ymm0
      add rdx,4*8
      add rbx,4*8
      dec rcx
      jnz @b

      ;sub 1 from the curlabel gb2, zero if v2 was zero
      mov rax,[labels]
      mov rdx,[gb2]

      vmovss xmm1, dword [one]
      vxorps ymm0,ymm0,ymm0

      rept 10 n:0 \{
	   movzx rbx,byte [rax+r10+n]
	   vmovss xmm2, [rdx+rbx*4+n*s2*4]
	   vcmpnless xmm3,xmm2,xmm0
	   vsubss xmm2,xmm2,xmm1
	   vandps ymm2,ymm2,ymm3
	   vmovss [rdx+rbx*4+n*s2*4],xmm2
      \}

	;zero gb1
	mov rax,[gb1]
	mov rcx,s1/8
	vxorps ymm0,ymm0,ymm0
	@@:
	rept 10 n:0 \{
	     vmovaps [rax+s1*4*n],ymm0
	\}
	add rax,4*8
	dec rcx
	jnz @b
    end if
    ThreadBarrier rax

      ;calc gb1 and update w2
      mov rax,[w2]
      mov rdx,[gb2]
      mov rcx, s2
      ;mov rbx,[gw2]
      vbroadcastss ymm12,dword [learningrate]

	gw1loop:
	rept 10 n:0 \{
	     vbroadcastss ymm\#n,[rdx+s2*4*n]
	\}

	mov r8,[v1]
	mov rsi,[gb1]
	mov r9,s1/8/2
	@@:
		vmovaps ymm10,[r8+ThreadCount*s1/2*4]
		vmulps ymm11,ymm10,ymm0
		;vmovaps [rbx],ymm11 ;test

		vmovaps ymm13,[rax+ThreadCount*s1/2*4]

		vmovaps ymm15,[rsi+ThreadCount*s1/2*4]
		vmulps ymm14,ymm0,ymm13
		vaddps ymm15,ymm15,ymm14
		vmovaps [rsi+ThreadCount*s1/2*4],ymm15

		rept 9 n:1 \{
		     vmovaps ymm10,[r8+s1*4*n+ThreadCount*s1/2*4]  ;gradient for weights, averaged
		     vmulps ymm10,ymm\#n,ymm10
		     vaddps ymm11,ymm11,ymm10

		     vmovaps ymm15,[rsi+s1*4*n+ThreadCount*s1/2*4] ;gradient for b1
		     vmulps ymm14,ymm\#n,ymm13
		     vaddps ymm15,ymm15,ymm14
		     vmovaps [rsi+s1*4*n+ThreadCount*s1/2*4],ymm15

		\}
		vmulps ymm11,ymm11,ymm12
		vsubps ymm13,ymm13,ymm11
		vmovaps [rax+ThreadCount*s1/2*4],ymm13

		add rax,4*8
		add r8,4*8
		add rsi,4*8
		dec r9
		jnz @b
	add rax,s1/2*4
	add rdx,4
	dec rcx
	jnz gw1loop

	;zero gb1 where v1 is zero
	mov rax, [v1]
	mov rbx, [gb1]
	mov rcx,s1/8/2
	vxorps ymm0,ymm0,ymm0
	@@: rept 10 n:0 \{
		 vmovaps ymm1,[rax+4*s1*n+ThreadCount*s1/2*4]
		 vcmpnleps ymm2,ymm1,ymm0
		 vmovaps ymm3,[rbx+4*s1*n+ThreadCount*s1/2*4]
		 vandps ymm3,ymm3,ymm2
		 vmovaps [rbx+4*s1*n+ThreadCount*s1/2*4],ymm3
	\}
	add rax,4*8
	add rbx,4*8
	dec rcx
	jnz @b

	ThreadBarrier rax

	;update w1
	mov rax,[gb1]
	mov rbx,[dat]
	add rbx,r13
	mov rdx,[w1]

	vbroadcastss ymm13,dword [learningrate]
	mov rsi,s1/2
	gw2loop:
		rept 10 n:0 \{
		     vbroadcastss ymm\#n,[rax+s1*4*n+ThreadCount*s1/2*4]
		\}
		mov r8,rbx
		mov rcx,s0/8/2
		@@:
		vmovaps ymm10,[r8]
		vmovaps ymm14,[rdx+ThreadCount*s0*s1/2*4]
		vmulps ymm11,ymm10,ymm0
		rept 9 n:1 \{
		     vmovaps ymm10,[r8+s0*4*n]
		     vmulps ymm12,ymm\#n,ymm10
		     vaddps ymm11,ymm11,ymm12

		\}
		vmulps ymm11,ymm11,ymm13
		vsubps ymm14,ymm14,ymm11
		vmovaps [rdx+ThreadCount*s0*s1/2*4],ymm14
		add r8,4*8
		add rdx,4*8
		dec rcx
		jnz @b
		add rdx,4*s0/2
		add rax,4
		dec rsi
		jnz gw2loop

	;round 2 with w1
	mov rax,[gb1]
	mov rbx,[dat]
	add rbx,r13
	add rbx,4*s0/2
	mov rdx,[w1]

	vbroadcastss ymm13,dword [learningrate]
	mov rsi,s1/2
	gw22loop:
		rept 10 n:0 \{
		     vbroadcastss ymm\#n,[rax+s1*4*n+ThreadCount*s1/2*4]
		\}
		mov r8,rbx
		add rdx,4*s0/2
		mov rcx,s0/8/2
		@@:
		vmovaps ymm10,[r8]
		vmovaps ymm14,[rdx+ThreadCount*s0*s1/2*4]
		vmulps ymm11,ymm10,ymm0
		rept 9 n:1 \{
		     vmovaps ymm10,[r8+s0*4*n]
		     vmulps ymm12,ymm\#n,ymm10
		     vaddps ymm11,ymm11,ymm12

		\}
		vmulps ymm11,ymm11,ymm13
		vsubps ymm14,ymm14,ymm11
		vmovaps [rdx+ThreadCount*s0*s1/2*4],ymm14
		add r8,4*8
		add rdx,4*8
		dec rcx
		jnz @b
		add rax,4
		dec rsi
		jnz gw22loop
	ThreadBarrier rax

	;update b1
	mov rax,[gb1]
	mov rbx,[b1]
	mov rcx,s1/8/2
	vbroadcastss ymm13,dword [learningrate]

	@@:
	vmovaps ymm2,[rbx+ThreadCount*s1/2*4]
	vxorps ymm1,ymm1,ymm1
	rept 10 n:0 \{
	     vmovaps ymm0,[rax+s1*4*n+ThreadCount*s1/2*4]
	     vaddps ymm1,ymm1,ymm0
	\}

	vmulps ymm0,ymm1,ymm13
	vsubps ymm2,ymm2,ymm0
	vmovaps [rbx+ThreadCount*s1/2*4],ymm2


	add rax,4*8
	add rbx,4*8
	dec rcx
	jnz @b

    if ThreadCount eq 0
	;update b2
	mov rax,[gb2]
	mov rbx,[b2]
	vbroadcastss ymm13,dword [learningrate]

	vmovaps ymm2,[rbx]
	vxorps ymm1,ymm1,ymm1
	rept 10 n:0 \{
	     vmovups ymm0,[rax+s2*4*n]
	     vaddps ymm1,ymm1,ymm0
	\}
	vmulps ymm0,ymm1,ymm13
	vsubps ymm2,ymm2,ymm0
	vmovaps [rbx],ymm2
	add rax,4*8
	add rbx,4*8

	vmovaps ymm2,[rbx]
	vxorps ymm1,ymm1,ymm1
	rept 10 n:0 \{
	     vmovups ymm0,[rax+s2*4*n]
	     vaddps ymm1,ymm1,ymm0
	\}
	vmulps ymm0,ymm1,ymm13
	vsubps ymm2,ymm2,ymm0
	vmovss [rbx],xmm2
	vshufps xmm2,xmm2,xmm2,1b
	vmovss [rbx+4],xmm2
    end if

}
macro pushall{
      \rept 8 n:8\{
	   push r\#n
      \}

      irp n, a,b,c,d \{
	  push r\#n\#x
      \}
      push rdi
      push rsi
}
macro popall{
      pop rsi
      pop rdi
      \irp n, d,c,b,a \{
	  pop r\#n\#x
      \}
      \rept 8 n:8\{
	   \reverse pop r\#n
      \}
}
macro testwholeset ThreadCount{
	local looper, looper2, looper3
  if ThreadCount eq 0
      xor r10,r10
      xor r13,r13
      mov [curoffset],r13
      xor r11,r11
      looper:
	    ThreadBarrier rax
	    \feedf 0
	    ThreadBarrier rax
	    mov rdx,10
	    mov rsi,[labels]
	    mov rax,[v2]
	    looper3:
	    xor rcx,rcx
	    xor rbx,rbx
	    vxorps xmm0,xmm0,xmm0
	    looper2: vmovss xmm1,[rax]
	    vcmpnless xmm2,xmm1,xmm0
	    vmovmskps rdi,xmm2
	    test rdi,1
	    jz @f
	    vmovss xmm0,xmm1,xmm1
	    mov rbx,rcx
	    @@: add rax,4
	    inc rcx
	    cmp rcx,10
	    jb looper2
	    movzx rcx,byte [rsi+r10]
	    inc rsi
	    cmp rcx,rbx
	    jnz @f
	    inc r11
	    @@: dec rdx
	    jnz looper3
	    add r10,10
	    add r13,s0*4*10
	    mov [curoffset],r13
	    cmp r10,10000
	    jb looper
      invoke printf,"Whole test set correct %d / 10000",r11
      invoke printf,<0xa,0>
   else
      xor r10,r10
     looper:
	    ThreadBarrier rax
	    \feedf 1
	    ThreadBarrier rax
	    add r10,10
	    cmp r10,10000
	    jb looper
   end if
}

macro movestuff{
@@: vmovaps ymm0,[rax]
vmovaps [rbx],ymm0
add rax,4*8
add rbx,4*8
dec rcx
jnz @b }

macro addstuff{
@@: vmovaps ymm0,[rbx]
vaddps ymm0,ymm0,[rax]
vmovaps [rbx],ymm0
add rax,4*8
add rbx,4*8
dec rcx
jnz @b }

macro updateweights{
@@: vmovaps ymm2,[rax]
vmovaps ymm3,[rbx]
vdivps ymm2,ymm2,ymm1
vmulps ymm2,ymm2,ymm0
vsubps ymm3,ymm3,ymm2
vmovaps [rbx],ymm3
add rax,4*8
add rbx,4*8
dec rcx
jnz @b }

macro writeweights{
invoke CreateFile, f3n,GENERIC_WRITE,FILE_SHARE_WRITE,0,CREATE_ALWAYS,FILE_ATTRIBUTE_NORMAL,0
mov [f3],rax
mov rax,[w1]
mov rbx,[b1]
mov rcx,s1
sub rsp,8
@@: push rax
push rcx
push rbx
invoke WriteFile,[f3],rax,s0*4,temp2,0
pop rbx
push rbx
invoke WriteFile,[f3],rbx,4,temp2,0
pop rbx
pop rcx
pop rax
add rbx,4
add rax,s0*4
dec rcx
jnz @b

mov rax,[w2]
mov rbx,[b2]
mov rcx,s2
@@: push rax
push rcx
push rbx
invoke WriteFile,[f3],rax,s1*4,temp2,0
pop rbx
push rbx
invoke WriteFile,[f3],rbx,4,temp2,0
pop rbx
pop rcx
pop rax
add rbx,4
add rax,s1*4
dec rcx
jnz @b
add rsp,8
invoke CloseHandle,[f3]
}

macro readweights{
invoke CreateFile, f3n,GENERIC_READ,FILE_SHARE_WRITE,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0
mov [f3],rax
mov rax,[w1]
mov rbx,[b1]
mov rcx,s1

sub rsp,8
@@: push rax
push rcx
push rbx
invoke ReadFile,[f3],rax,s0*4,temp2,0
pop rbx
push rbx
invoke ReadFile,[f3],rbx,4,temp2,0
pop rbx
pop rcx
pop rax
add rbx,4
add rax,s0*4
dec rcx
jnz @b

mov rax,[w2]
mov rbx,[b2]
mov rcx,s2
@@: push rax
push rcx
push rbx
invoke ReadFile,[f3],rax,s1*4,temp2,0
pop rbx
push rbx
invoke ReadFile,[f3],rbx,4,temp2,0
pop rbx
pop rcx
pop rax
add rbx,4
add rax,s1*4
dec rcx
jnz @b
add rsp,8
invoke CloseHandle,[f3]
}

macro printv0{
  local loop
mov rax,[dat]
add rax,r13
mov rbx,28
loop:
mov rcx,28
push rbx
@@: push rcx
push rax
fld dword [rax]
fstp qword [temp]
invoke printf,"%.2f ",qword [temp]
pop rax
pop rcx
add rax,4
dec rcx
jnz @b
push rcx
push rax
invoke printf,<0xa,0>
pop rax
pop rcx
pop rbx
dec rbx
jnz loop
invoke printf,"Curlabel %d ",rbp
invoke printf,<0xa,0>
}
macro printv1 address=[v1],n=0{
  local loop
mov rax,address
mov rbx,10
loop:
mov rcx,10
push rbx
@@: push rcx
push rax
fld dword [rax+n*s1*4]
fstp qword [temp]
invoke printf,"%.5f ",qword [temp]
pop rax
pop rcx
add rax,4
dec rcx
jnz @b
push rcx
push rax
invoke printf,<0xa,0>
pop rax
pop rcx
pop rbx
dec rbx
jnz loop
invoke printf,"Curlabel %d ",rbp
invoke printf,<0xa,0>
}
macro printv2 address=[v2],n=0{
mov rax,address
mov rcx,10
@@: push rcx
push rax
fld dword [rax+s2*4*n]
fstp qword [temp]
invoke printf,"%.5f ",qword [temp]
pop rax
pop rcx
add rax,4
dec rcx
jnz @b
invoke printf,<0xa,0>
invoke printf,"Curlabel %d ",rbp
invoke printf,<0xa,0>
}
macro printw w,shift, n{
mov r15,w
add r15,shift
mov r14,n
@@: fld dword [r15]
fstp qword [temp]
invoke printf,"%.5f ",qword [temp]
add r15,4
dec r14
jnz @b
invoke printf,<0xa,0>
}
macro printall{
random
feedf
printv0
printv1
printv2
}
macro printtime{
invoke QueryPerformanceCounter,ended
fild qword [freq]
fild qword [ended]
fild qword [start]
fsubp st1,st0
fdiv st0,st1
fstp qword [temp]
finit
invoke printf,<"%.6f ",0xa>,[temp]
}
macro randomall{
	mov rbx,s0*s1
	mov rdi,[w1]
	randomizeweights
	mov rbx,s1*s2
	mov rdi,[w2]
	randomizeweights
	mov rbx,s1
	mov rdi,[b1]
	randomizeweights
	mov rbx,s2
	mov rdi,[b2]
	randomizeweights
	normalize
}

macro test2{
      local feed1, feed2
mov rax,[w1]
mov rdi,[v1]
mov rdx,[b1]
mov rbx,s1
mov r8,[dat]
add r8,r13
    feed1:
vzeroall
mov rcx,s0/8
mov r9,r8
@@: vmovaps ymm10,[rax]

rept 10 n:0 \{
   rept 1 m:11+(n mod 5) \\{
	vmovaps ymm\\#m,[r9+s0*4*n]
	vmulps ymm\\#m,ymm\\#m,ymm10
	vaddps ymm\#n,ymm\#n,ymm\\#m
   \\}
\}
    add rax,4*8
    add r9,4*8
    dec rcx
    jnz @b

rept 10 n:0
\{
       vhaddps ymm10,ymm\#n,ymm\#n
       vhaddps ymm10,ymm10,ymm10
       vperm2f128 ymm11,ymm10,ymm10,0x11
       vaddps ymm10,ymm10,ymm11
       vmovss [rdi+s1*4*n],xmm10
\}

	add rdi,4
	add rax,4*8
	dec rbx
	jnz feed1

}

macro testspeed{
;from first layer to the second, curOffset is in r13
      local feed1, feed12, feed2
mov rax,[w1]
mov rdi,[v1]
mov rdx,[b1]
mov rbx,s1
mov r8,[dat]
add r8,r13
    feed1:
vzeroall
mov rcx,s0/8/2
mov r9,r8
@@: ;vmovaps ymm10,[rax]

rept 10 n:0 \{
	vmovaps ymm11,[r9+s0*4*n]
\}
    add rax,4*8
    add r9,4*8
    dec rcx
    jnz @b


	add rax,4*s0/2
	dec rbx
	jnz feed1

mov rax,[w1]
mov rdi,[v1]
mov rdx,[b1]
mov rbx,s1
add r8,4*s0/2
    feed12:
vzeroall
mov rcx,s0/8/2
add rax,4*s0/2
mov r9,r8
@@: ;vmovaps ymm10,[rax]

rept 10 n:0 \{
	vmovaps ymm11,[r9+s0*4*n]
\}
    add rax,4*8
    add r9,4*8
    dec rcx
    jnz @b

	dec rbx
	jnz feed12

}
;}}}}}}}}}}}}}} MACROS ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
fld qword [one]
fstp dword [one]
fld qword [learningrate]
fld qword [stocount]
fdivp st1,st0
fstp dword [learningrate]

invoke QueryPerformanceFrequency, freq


	loaddata trainingdata
	allocweights
	randomall
	invoke CreateThread,0,0,ThreadProc0,0,0,0
	mov [hThread0],rax
	invoke CreateThread,0,0,ThreadProc1,0,0,0
	mov [hThread1],rax
	invoke WaitForMultipleObjects,2,hThread0,TRUE,-1
	jmp p

p: invoke printf,"END",
invoke Sleep,-1
invoke ExitProcess,0

loopcount=6000*100

ThreadProc0: sub rsp,8

	   ;readweights
	   invoke QueryPerformanceCounter,start
	   mov r15,loopcount
  rept 1 {
  loop0:
	   randompic
	   ThreadBarrier rax
	   feedf 0
	   ThreadBarrier rax
	   backprop 0
	   dec r15
	   jnz loop0	       }

	   ThreadBarrier rax
	   invoke QueryPerformanceCounter,ended
	   printtime
	   loaddata testdata
	   testwholeset 0
	   ;writeweights

	   add rsp,8
	   ret


ThreadProc1: sub rsp,8

	   mov r15,loopcount
  rept 1 {
  loop1:
	   ThreadBarrier rax
	   feedf 1
	   ThreadBarrier rax
	   backprop 1
	   dec r15
	   jnz loop1	    }

	   ThreadBarrier rax
	   testwholeset 1

	   add rsp,8
	   ret

section '.data' data readable writeable
dat dq 0
dattest dq 0
dattraining dq 0
dattemp dq 0
labels dq 0
labelstraining dq 0
labelstest dq 0
labelstemp dq 0
picnum dq 0
curpic dq 0
curoffset dq 0
picnumtest dq 0
picnumtraining dq 0
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
f2n db 'D:\Java\Projekt\train-labels.idx1-ubyte',0
f3n db 'Weightsasmfloatfastmultithread.txt',0
f4n db 'D:\Java\Projekt\t10k-images.idx3-ubyte',0
f5n db 'D:\Java\Projekt\t10k-labels.idx1-ubyte',0
align 16
f1s dq 0
f2s dq 0
f4s dq 0
f5s dq 0
f1 dq 0
f2 dq 0
f3 dq 0
f4 dq 0
f5 dq 0
align 32
temp dq 0,0
temp2 dq 0,0
powtwo dq -63
maxintensity dq 255
allones dq 2
freq dq 0
start dq 0
ended dq 0
stocount dq 10.0f
learningrate dq 0.02f	  ;orig:0.05f
one dq 1.0f
hThread0 dq 0
hThread1 dq 0
lock1 dq 0
lock2 dq THREADCOUNT

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
	      rand_s,'rand_s',\
	      rand,'rand'