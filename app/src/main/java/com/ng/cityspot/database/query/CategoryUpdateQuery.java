package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.CategoryDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.CategoryModel;

import java.sql.SQLException;

public class CategoryUpdateQuery extends Query {
	private CategoryModel mCategory;

	public CategoryUpdateQuery(CategoryModel category) {
		mCategory = category;
	}

	@Override
	public Data<Integer> processData() throws SQLException {
		Data<Integer> data = new Data<>();
		data.setDataObject(CategoryDAO.update(mCategory));
		return data;
	}
}
