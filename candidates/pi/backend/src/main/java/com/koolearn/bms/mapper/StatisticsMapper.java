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

    /** 呆滞物料：超过 cutoff 时间无出库记录，返回最后出库时间、库存、库存金额 */
    @Select("SELECT m.id AS materialId, m.material_code AS materialCode, m.material_name AS materialName, "
          + "m.package_type AS packageType, m.spec_model AS specModel, m.location_no AS locationNo, "
          + "m.stock AS stock, m.material_cost AS materialCost, "
          + "(m.stock * COALESCE(m.material_cost, 0)) AS amount, "
          + "r.last_out_time AS lastOutTime "
          + "FROM tb_material m "
          + "LEFT JOIN (SELECT material_id, MAX(out_time) AS last_out_time FROM tb_out_record GROUP BY material_id) r ON m.id = r.material_id "
          + "WHERE m.stock > 0 AND (r.last_out_time IS NULL OR r.last_out_time < #{cutoff}) "
          + "ORDER BY r.last_out_time ASC")
    List<Map<String, Object>> stagnantMaterials(@Param("cutoff") String cutoff);

    /** 按供应商统计入库 */
    @Select("SELECT o.supplier AS supplier, COUNT(*) AS in_count, COALESCE(SUM(r.in_num),0) AS total_in "
          + "FROM in_record r JOIN tb_inbound_order o ON r.bill_no = o.bill_no "
          + "WHERE r.in_time BETWEEN #{start} AND #{end} AND (o.supplier IS NOT NULL AND o.supplier <> '') "
          + "GROUP BY o.supplier ORDER BY total_in DESC")
    List<Map<String, Object>> inboundStatsBySupplier(@Param("start") String start, @Param("end") String end);

    /** 按领料部门统计出库（out_user -> sys_user.dept） */
    @Select("SELECT u.dept AS dept, COUNT(*) AS out_count, COALESCE(SUM(r.out_num),0) AS total_out "
          + "FROM tb_out_record r LEFT JOIN sys_user u ON r.out_user = u.username "
          + "WHERE r.out_time BETWEEN #{start} AND #{end} "
          + "GROUP BY u.dept ORDER BY total_out DESC")
    List<Map<String, Object>> outboundStatsByDept(@Param("start") String start, @Param("end") String end);

    /** 物料汇总：累计入库/次数/平均批次数量、累计出库/次数/最后出库时间、当前库存 */
    @Select("SELECT m.id AS materialId, m.material_code AS materialCode, m.material_name AS materialName, "
          + "m.package_type AS packageType, m.spec_model AS specModel, m.location_no AS locationNo, "
          + "m.stock AS stock, m.lock_stock AS lockStock, m.min_stock AS minStock, m.max_stock AS maxStock, "
          + "COALESCE(i.total_in,0) AS totalIn, COALESCE(i.in_count,0) AS inCount, COALESCE(i.avg_batch,0) AS avgBatch, "
          + "COALESCE(o.total_out,0) AS totalOut, COALESCE(o.out_count,0) AS outCount, o.last_out_time AS lastOutTime "
          + "FROM tb_material m "
          + "LEFT JOIN (SELECT material_id, COALESCE(SUM(in_num),0) AS total_in, COUNT(*) AS in_count, "
          + "                  COALESCE(SUM(in_num)/NULLIF(COUNT(*),0),0) AS avg_batch "
          + "           FROM in_record GROUP BY material_id) i ON i.material_id = m.id "
          + "LEFT JOIN (SELECT material_id, COALESCE(SUM(out_num),0) AS total_out, COUNT(*) AS out_count, MAX(out_time) AS last_out_time "
          + "           FROM tb_out_record GROUP BY material_id) o ON o.material_id = m.id "
          + "ORDER BY m.material_code")
    List<Map<String, Object>> materialSummary();
}
