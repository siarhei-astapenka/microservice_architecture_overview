package com.epam.learn.storageservice.mapper;

import com.epam.learn.storageservice.entity.StorageEntity;
import com.epam.learn.storageservice.model.Storage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    StorageMapper INSTANCE = Mappers.getMapper(StorageMapper.class);

    StorageEntity toStorageEntity(Storage storage);

    Storage toStorage(StorageEntity entity);

    List<Storage> toStorageList(List<StorageEntity> entities);
}
