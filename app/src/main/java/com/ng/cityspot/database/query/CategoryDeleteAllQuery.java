package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.CategoryDAO;
import com.ng.cityspot.database.data.Data;

import java.sql.SQLException;

public class CategoryDeleteAllQuery extends Query {
	public CategoryDeleteAllQuery() {
	}

	@Override
	public Data<Integer> processData() throws SQLException {
		Data<Integer> data = new Data<>();
		data.setDataObject(CategoryDAO.deleteAll());
		return data;
	}
}
