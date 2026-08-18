package com.koolearn.bms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koolearn.bms.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
    //分页多条件查询：编码/名称/仓库编码模糊
    IPage<Material> selectPageByCondition(Page<Material> page,
                                          @Param("code") String materialCode,
                                          @Param("name") String materialName,
                                          @Param("warehouse") String warehouseCode,
                                          @Param("keyword") String keyword);

    //原子增加库存
    int atomicAddStock(@Param("mid") Long materialId, @Param("num") BigDecimal num, @Param("ver") Integer version);
    //原子扣减库存
    int atomicSubStock(@Param("mid") Long materialId, @Param("num") BigDecimal num, @Param("ver") Integer version);
    //锁定库存
    int lockStock(@Param("mid") Long materialId, @Param("num") BigDecimal num, @Param("ver") Integer version);
    //解锁库存
    int unLockStock(@Param("mid") Long materialId, @Param("num") BigDecimal num, @Param("ver") Integer version);

    /**
     * 锁定读（当前读）：SELECT ... FOR UPDATE。
     * MySQL REPEATABLE READ 下普通 SELECT 走快照，重试会一直读到旧 version；
     * FOR UPDATE 为当前读，总是读取最新已提交数据，保证重试能拿到新版本。
     */
    Material selectByIdForUpdate(@Param("id") Long id);
}