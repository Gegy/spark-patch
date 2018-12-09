package com.hrznstudio.spark.patch;

/**
 * A bytecode patcher acting on raw class bytes
 */
public interface IBytePatcher {
    /**
     * Transforms the given class bytes.
     *
     * @param target the fully qualified class name being transformed
     * @param bytes the input bytes of this class
     * @return transformed bytes for this class
     */
    byte[] apply(String target, byte[] bytes);
}
