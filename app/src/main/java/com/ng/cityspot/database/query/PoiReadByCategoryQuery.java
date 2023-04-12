package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.PoiDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.PoiModel;

import java.sql.SQLException;
import java.util.List;

public class PoiReadByCategoryQuery extends Query {
	private long mCategoryId;
	private long mSkip = -1L;
	private long mTake = -1L;

	public PoiReadByCategoryQuery(long categoryId) {
		mCategoryId = categoryId;
	}

	public PoiReadByCategoryQuery(long categoryId, long skip, long take) {
		mCategoryId = categoryId;
		mSkip = skip;
		mTake = take;
	}

	@Override
	public Data<List<PoiModel>> processData() throws SQLException {
		Data<List<PoiModel>> data = new Data<>();
		data.setDataObject(PoiDAO.readByCategory(mCategoryId, mSkip, mTake));
		return data;
	}
}
