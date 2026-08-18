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
          + "m.stock, COALESCE(r.last_out, 0) AS lastOutDays "
          + "FROM tb_material m "
          + "LEFT JOIN (SELECT material_id, DATEDIFF(NOW(), MAX(out_time)) AS last_out FROM tb_out_record GROUP BY material_id) r ON m.id = r.material_id "
          + "WHERE (r.last_out IS NULL OR r.last_out > #{days}) AND m.stock > 0 "
          + "ORDER BY r.last_out DESC")
    List<Map<String, Object>> stagnantMaterials(@Param("days") int days);
}
