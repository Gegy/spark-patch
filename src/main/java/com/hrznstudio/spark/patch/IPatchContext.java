package com.hrznstudio.spark.patch;

import java.io.IOException;

public interface IPatchContext {
    byte[] readRawBytes(String name) throws IOException;

    byte[] readClasspathBytes(String name) throws IOException;
}
