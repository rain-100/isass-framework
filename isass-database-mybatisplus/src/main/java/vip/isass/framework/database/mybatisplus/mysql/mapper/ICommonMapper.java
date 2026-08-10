// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Mapper
public interface ICommonMapper {

    List<Map<String, Object>> findAllSubRecords(String tableName,
                                                String idColumnName,
                                                String parentIdColumnName,
                                                Serializable id,
                                                boolean returnIdRecord,
                                                String logicDeleteSql,
                                                List<String> columnNameList);

}