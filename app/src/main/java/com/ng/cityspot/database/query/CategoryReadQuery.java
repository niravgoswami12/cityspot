package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.CategoryDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.CategoryModel;

import java.sql.SQLException;

public class CategoryReadQuery extends Query {
	private long mId;

	public CategoryReadQuery(long id) {
		mId = id;
	}

	@Override
	public Data<CategoryModel> processData() throws SQLException {
		Data<CategoryModel> data = new Data<>();
		data.setDataObject(CategoryDAO.read(mId));
		return data;
	}
}
