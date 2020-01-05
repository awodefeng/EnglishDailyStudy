package com.xiaoxun.englishdailystudy.utils;

public class SentenceStruct {
    private int id;
    private String sentence_txt;
    private String pronounce;

    private int score;

    public SentenceStruct(){}

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setSentence_txt(String sentence_txt) {
        this.sentence_txt = sentence_txt;
    }

    public String getSentence_txt() {
        return sentence_txt;
    }

    public void setPronounce(String pronounce) {
        this.pronounce = pronounce;
    }

    public String getPronounce() {
        return pronounce;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
