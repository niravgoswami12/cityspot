package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.PoiDAO;
import com.ng.cityspot.database.data.Data;

import java.sql.SQLException;

public class PoiDeleteAllQuery extends Query {
	public PoiDeleteAllQuery() {
	}

	@Override
	public Data<Integer> processData() throws SQLException {
		Data<Integer> data = new Data<>();
		data.setDataObject(PoiDAO.deleteAll());
		return data;
	}
}
