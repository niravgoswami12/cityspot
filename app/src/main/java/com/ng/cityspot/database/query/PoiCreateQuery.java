package com.ng.cityspot.database.query;

import com.ng.cityspot.database.dao.PoiDAO;
import com.ng.cityspot.database.data.Data;
import com.ng.cityspot.database.model.PoiModel;

import java.sql.SQLException;

public class PoiCreateQuery extends Query {
	private PoiModel mPoi;

	public PoiCreateQuery(PoiModel poi) {
		mPoi = poi;
	}

	@Override
	public Data<Integer> processData() throws SQLException {
		Data<Integer> data = new Data<>();
		data.setDataObject(PoiDAO.create(mPoi));
		return data;
	}
}
