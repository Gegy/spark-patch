package com.hrznstudio.spark.patch;

import java.util.Collection;

public interface IPatchPlugin {
    void initialize();

    Collection<IBytePatcher> getPatchers();
}
