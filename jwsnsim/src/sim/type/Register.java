/*
 * Copyright (c) 2014, Ege University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the copyright holder nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package sim.type;

/**
 *
 * Simulates 32 bit unsigned data type for the Java language.
 * (Especially used for the hardware registers.) 
 * 
 * @author Kasım Sinan YILDIRIM (sinanyil81@gmail.com)
 *
 */
public class Register implements Comparable<Register>{
	
	/** Maximum possible value. */
	public static final long MAX_VALUE = 0xFFFFFFFFL;
	
	/** Minimum possible value. */
	public static final long MIN_VALUE = 0;

	private long value;

	public Register() {
		this.value = 0;
	}
	
	public Register(Register value) {
		this.value = value.toLong();
	}
	
	public Register(long value) {
		this.value = value & MAX_VALUE;
	}
	
	public Register(int value) {
		/* get all bits other than the sign bit */		
		this.value = value & 0x7FFFFFFFL;
		
		if(value < 0){			
			this.value |= 0x80000000L;
		}
		else{
			this.value = value;
		}		
	}
	
	public Register add(Register x){
		long result = value + x.toLong();
		
		if(result > MAX_VALUE){
			result -= MAX_VALUE;
			result--;
		}
					
		return new Register(result);
	}
	
	public Register add(int x){
		return add(new Register(x));
	}
	
	public Register twosComplement(){
		long result = value;
		
		/* inverse of 2's complement */
		result = (~result) & MAX_VALUE;
		
		if(result == MAX_VALUE){
			result = 0;
		}
		else{
			result++;
		}
		
		return new Register(result);
	}
	
	public Register subtract(Register x){
		
		Register twosComplementOfX = x.twosComplement();
					
		return add(twosComplementOfX);
	}
	
	public Register subtract(int x){
		return subtract(new Register(x));
	}
	
	public Register multiply(float x){		
		
		return new Register((int)(x*this.value));
	}

	public long toLong(){
		return value;
	}
	
	public int toInteger(){
		
		return (int)value;
	}
	
	public double toDouble(){
		
		return (double)((int)value);
	}

	/** The value of this as a string. */
	@Override
	public String toString() {
		return "" + value;
	}

	@Override
	public int compareTo(Register o) {
		return (int)(value - o.toLong());
	}

}
