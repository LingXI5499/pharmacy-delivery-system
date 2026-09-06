package com.pharmacy.service;
import com.pharmacy.common.PageData;
import com.pharmacy.dto.CategoryRequest;
import com.pharmacy.vo.CategoryVO;
public interface CategoryService { PageData<CategoryVO> page(long page,long size,String keyword,Integer status); CategoryVO create(CategoryRequest request); CategoryVO update(Long id,CategoryRequest request); void delete(Long id); void updateStatus(Long id,Integer status); }
