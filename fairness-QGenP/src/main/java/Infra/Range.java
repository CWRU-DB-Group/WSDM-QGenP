package Infra;

import java.util.HashSet;

public class Range {
    public double low;
    public double high;
    HashSet<String> domain;
    public Range(double low, double high) {
        this.low= low;
        this.high = high;
        this.domain = new HashSet<>();
    }
    public Range(HashSet<String> domain) {
        this.low= 0;
        this.high = 0;
        this.domain = domain;
    }
}
