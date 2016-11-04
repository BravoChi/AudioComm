package com.libra.sinvoice;

/**
 * Created by v-wenchi on 9/9/2015.
 */
public class FreqPair {
    private int freq1;
    private int freq2;
    private int index1;
    private int index2;
    private final static int[] CODE_FREQUENCY = { 17851, 18174,18497,18820,19143,19466,19789,19294,19638};

    public FreqPair(int index1, int index2, boolean index){
        this.index1 = index1;
        this.index2 = index2;
        this.freq1 = CODE_FREQUENCY[this.index1];
        this.freq2 = CODE_FREQUENCY[this.index2];
    }

    public FreqPair(int freq1, int freq2){
        this.freq1 = freq1;
        this.freq2 = freq2;
    }
    public boolean compare(FreqPair p1){

        if((getFreq1(p1)==this.freq1)&&(getFreq2(p1)==this.freq2)||(getFreq2(p1)==this.freq1&&(getFreq1(p1))==this.freq2))
            return true;
        return false;
    }
    public int getIndex1(FreqPair pair){
        return pair.index1;
    }
    public int getIndex2(FreqPair pair){
        return pair.index2;
    }
    public int getFreq1(FreqPair pair){
        return pair.freq1;
    }
    public int getFreq2(FreqPair pair){
        return pair.freq2;
    }
    public String toString(){
        return "Freq1: "+freq1+"; Freq2: "+freq2;
    }
    //public void setFreq(int freq1,int freq2){}


}
