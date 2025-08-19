package com.sadanah.floro;

import androidx.annotation.Nullable;

import java.util.List;

public class Article {
    public int id;
    public String title;
    public String subtitle;
    public List<String> content;
    public String image;
    public List<String> tags;
    public String date;
    @Nullable
    public String link;
    @Nullable public Double price_lkr;
}

