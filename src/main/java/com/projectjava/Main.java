package com.projectjava;

import ij.IJ;
import ij.ImagePlus;

public class Main {
    public static void main(String[] args) {
        ImagePlus imp = IJ.openImage();
        imp.show();
    }
}
