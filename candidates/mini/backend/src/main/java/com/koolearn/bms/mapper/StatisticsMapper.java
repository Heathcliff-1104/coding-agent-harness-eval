package com.koolearn.bms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT DATE_FORMAT(r.in_time, #{fmt}) AS period, COUNT(*) AS cnt, COALESCE(SUM(r.in_num),0) AS total "
          + "FROM in_record r "
          + "WHERE r.in_time >= CONCAT(#{start}, ' 00:00:00') AND r.in_time <= CONCAT(#{end}, ' 23:59:59') "
          + "GROUP BY DATE_FORMAT(r.in_time, #{fmt}) ORDER BY period")
    List<Map<String, Object>> inboundStats(@Param("fmt") String fmt, @Param("start") String start, @Param("end") String end);

    @Select("SELECT DATE_FORMAT(r.out_time, #{fmt}) AS period, COUNT(*) AS cnt, COALESCE(SUM(r.out_num),0) AS total "
          + "FROM tb_out_record r "
          + "WHERE r.out_time >= CONCAT(#{start}, ' 00:00:00') AND r.out_time <= CONCAT(#{end}, ' 23:59:59') "
          + "GROUP BY DATE_FORMAT(r.out_time, #{fmt}) ORDER BY period")
    List<Map<String, Object>> outboundStats(@Param("fmt") String fmt, @Param("start") String start, @Param("end") String end);

    // 按物料统计入库：总数/次数/平均批量
    @Select("<script>"
          + "SELECT r.material_id AS materialId, m.material_code AS materialCode, m.material_name AS materialName, "
          + "COUNT(*) AS cnt, COALESCE(SUM(r.in_num),0) AS total, COALESCE(AVG(r.in_num),0) AS avgBatch "
          + "FROM in_record r LEFT JOIN tb_material m ON r.material_id = m.id "
          + "WHERE r.in_time >= CONCAT(#{start}, ' 00:00:00') AND r.in_time &lt;= CONCAT(#{end}, ' 23:59:59') "
          + "<if test='materialId != null'> AND r.material_id = #{materialId} </if> "
          + "GROUP BY r.material_id, m.material_code, m.material_name ORDER BY total DESC"
          + "</script>")
    List<Map<String, Object>> inboundStatsByMaterial(@Param("materialId") Long materialId,
                                                     @Param("start") String start, @Param("end") String end);

    // 按供应商统计入库（关联入库单）
    @Select("SELECT o.supplier AS supplier, COUNT(DISTINCT r.bill_no) AS cnt, COALESCE(SUM(r.in_num),0) AS total "
          + "FROM in_record r LEFT JOIN tb_inbound_order o ON r.bill_no = o.bill_no "
          + "WHERE r.in_time >= CONCAT(#{start}, ' 00:00:00') AND r.in_time <= CONCAT(#{end}, ' 23:59:59') "
          + "GROUP BY o.supplier ORDER BY total DESC")
    List<Map<String, Object>> inboundStatsBySupplier(@Param("start") String start, @Param("end") String end);

    // 按物料统计出库
    @Select("<script>"
          + "SELECT r.material_id AS materialId, m.material_code AS materialCode, m.material_name AS materialName, "
          + "COUNT(*) AS cnt, COALESCE(SUM(r.out_num),0) AS total "
          + "FROM tb_out_record r LEFT JOIN tb_material m ON r.material_id = m.id "
          + "WHERE r.out_time >= CONCAT(#{start}, ' 00:00:00') AND r.out_time &lt;= CONCAT(#{end}, ' 23:59:59') "
          + "<if test='materialId != null'> AND r.material_id = #{materialId} </if> "
          + "GROUP BY r.material_id, m.material_code, m.material_name ORDER BY total DESC"
          + "</script>")
    List<Map<String, Object>> outboundStatsByMaterial(@Param("materialId") Long materialId,
                                                      @Param("start") String start, @Param("end") String end);

    // 按领料部门统计出库（关联出库单申请部门 -> 用户部门）
    @Select("SELECT u.dept AS dept, COUNT(*) AS cnt, COALESCE(SUM(r.out_num),0) AS total "
          + "FROM tb_out_record r "
          + "LEFT JOIN tb_outbound_order o ON r.outbound_code = o.outbound_code "
          + "LEFT JOIN sys_user u ON o.apply_user = u.username "
          + "WHERE r.out_time >= CONCAT(#{start}, ' 00:00:00') AND r.out_time <= CONCAT(#{end}, ' 23:59:59') "
          + "GROUP BY u.dept ORDER BY total DESC")
    List<Map<String, Object>> outboundStatsByDept(@Param("start") String start, @Param("end") String end);

    // 呆滞物料：包含最后出库时间、库存金额
    @Select("SELECT m.id, m.material_code AS materialCode, m.material_name AS materialName, "
          + "m.stock, m.material_cost AS materialCost, "
          + "COALESCE(m.stock * COALESCE(m.material_cost,0),0) AS stockAmount, "
          + "MAX(r.out_time) AS lastOutTime, "
          + "DATEDIFF(NOW(), MAX(r.out_time)) AS lastOutDays "
          + "FROM tb_material m "
          + "LEFT JOIN tb_out_record r ON m.id = r.material_id "
          + "GROUP BY m.id, m.material_code, m.material_name, m.stock, m.material_cost "
          + "HAVING (MAX(r.out_time) IS NULL OR DATEDIFF(NOW(), MAX(r.out_time)) > #{days}) AND m.stock > 0 "
          + "ORDER BY lastOutDays DESC")
    List<Map<String, Object>> stagnantMaterials(@Param("days") int days);
}
