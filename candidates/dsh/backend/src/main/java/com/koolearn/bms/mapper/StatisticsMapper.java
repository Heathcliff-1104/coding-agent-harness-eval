package com.koolearn.bms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT DATE_FORMAT(in_time, #{fmt}) AS period, COUNT(*) AS cnt, COALESCE(SUM(in_num),0) AS total "
          + "FROM in_record WHERE in_time BETWEEN #{start} AND #{end} "
          + "GROUP BY DATE_FORMAT(in_time, #{fmt}) ORDER BY period")
    List<Map<String, Object>> inboundStats(@Param("fmt") String fmt, @Param("start") String start, @Param("end") String end);

    @Select("SELECT DATE_FORMAT(out_time, #{fmt}) AS period, COUNT(*) AS cnt, COALESCE(SUM(out_num),0) AS total "
          + "FROM tb_out_record WHERE out_time BETWEEN #{start} AND #{end} "
          + "GROUP BY DATE_FORMAT(out_time, #{fmt}) ORDER BY period")
    List<Map<String, Object>> outboundStats(@Param("fmt") String fmt, @Param("start") String start, @Param("end") String end);

    @Select("SELECT m.id, m.material_code AS materialCode, m.material_name AS materialName, "
          + "m.stock, COALESCE(r.last_out, 0) AS lastOutDays, "
          + "COALESCE(m.stock * m.material_cost, 0) AS stockAmount "
          + "FROM tb_material m "
          + "LEFT JOIN (SELECT material_id, DATEDIFF(NOW(), MAX(out_time)) AS last_out FROM tb_out_record GROUP BY material_id) r ON m.id = r.material_id "
          + "WHERE (r.last_out IS NULL OR r.last_out > #{days}) AND m.stock > 0 "
          + "ORDER BY r.last_out DESC")
    List<Map<String, Object>> stagnantMaterials(@Param("days") int days);

    /** 某物料在时间段内的入库总数、入库次数、平均批次数量。 */
    @Select("SELECT r.material_id AS materialId, r.material_code AS materialCode, r.material_name AS materialName, "
          + "COALESCE(SUM(r.in_num),0) AS totalIn, COUNT(*) AS inTimes, "
          + "COALESCE(AVG(r.in_num),0) AS avgBatchNum "
          + "FROM in_record r WHERE r.in_time BETWEEN #{start} AND #{end} "
          + "AND (#{materialId} IS NULL OR r.material_id = #{materialId}) "
          + "GROUP BY r.material_id, r.material_code, r.material_name ORDER BY totalIn DESC")
    List<Map<String, Object>> inboundStatsByMaterial(@Param("materialId") Long materialId,
                                                     @Param("start") String start, @Param("end") String end);

    /** 按供应商统计入库量。 */
    @Select("SELECT COALESCE(r.supplier,'未知') AS supplier, COUNT(*) AS cnt, COALESCE(SUM(r.in_num),0) AS total "
          + "FROM in_record r WHERE r.in_time BETWEEN #{start} AND #{end} "
          + "GROUP BY r.supplier ORDER BY total DESC")
    List<Map<String, Object>> inboundStatsBySupplier(@Param("start") String start, @Param("end") String end);

    /** 按物料统计出库量。 */
    @Select("SELECT r.material_id AS materialId, r.material_code AS materialCode, r.material_name AS materialName, "
          + "COALESCE(SUM(r.out_num),0) AS totalOut, COUNT(*) AS outTimes "
          + "FROM tb_out_record r WHERE r.out_time BETWEEN #{start} AND #{end} "
          + "AND (#{materialId} IS NULL OR r.material_id = #{materialId}) "
          + "GROUP BY r.material_id, r.material_code, r.material_name ORDER BY totalOut DESC")
    List<Map<String, Object>> outboundStatsByMaterial(@Param("materialId") Long materialId,
                                                      @Param("start") String start, @Param("end") String end);

    /** 按领料部门统计出库量。 */
    @Select("SELECT COALESCE(r.dept,'未知') AS dept, COUNT(*) AS cnt, COALESCE(SUM(r.out_num),0) AS total "
          + "FROM tb_out_record r WHERE r.out_time BETWEEN #{start} AND #{end} "
          + "GROUP BY r.dept ORDER BY total DESC")
    List<Map<String, Object>> outboundStatsByDept(@Param("start") String start, @Param("end") String end);

    /** 库存明细（报表导出用）。 */
    @Select("SELECT m.material_code AS materialCode, m.material_name AS materialName, m.package_type AS packageType, "
          + "m.value_data AS valueData, m.spec_model AS specModel, m.manufacturer, "
          + "m.stock, m.location_no AS locationNo, m.remark, m.lock_stock AS lockStock "
          + "FROM tb_material m ORDER BY m.material_code")
    List<Map<String, Object>> inventoryDetail();
}
