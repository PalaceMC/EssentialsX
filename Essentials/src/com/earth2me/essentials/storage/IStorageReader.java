package com.earth2me.essentials.storage;


public interface IStorageReader<T extends StorageObject> {
    T load(final Class<T> clazz) throws ObjectLoadException;
}
