package com.wikitude.samples.util;

import java.util.List;

public class SampleCategory {

    private final String name;
    private final List<SampleData> samples;

    public SampleCategory(String name, List<SampleData> samples) {
        this.name = name;
        this.samples = samples;
    }

    public String getName() {
        return name;
    }

    public List<SampleData> getSamples() {
        return samples;
    }
}
