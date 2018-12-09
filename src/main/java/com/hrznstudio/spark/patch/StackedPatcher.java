package com.hrznstudio.spark.patch;

import java.util.Collection;

public class StackedPatcher implements IBytePatcher {
    private final Collection<IBytePatcher> patchers;

    public StackedPatcher(Collection<IBytePatcher> patchers) {
        this.patchers = patchers;
    }

    @Override
    public byte[] apply(String target, byte[] bytes) {
        byte[] result = bytes;
        for (IBytePatcher patcher : this.patchers) {
            result = patcher.apply(target, result);
        }
        return result;
    }

    public Collection<IBytePatcher> getStacked() {
        return this.patchers;
    }
}
