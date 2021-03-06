package com.lujiahao.rest.service.impl;

import com.lujiahao.rest.pojo.CatNode;
import com.lujiahao.rest.pojo.CatResult;
import com.lujiahao.rest.service.ItemCatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lujiahao on 2017/3/28.
 */
@Service
public class ItemCatServiceImpl implements ItemCatService {
    @Autowired
    private TbItemCatMapper itemCatMapper;

    @Override
    public CatResult getItemCatList() {
        CatResult catResult = new CatResult();
        // 查询分类列表  查询根节点
        catResult.setData(getCatList(0));
        return catResult;
    }

    /**
     * 通过parentid来查询商品分类列表
     */
    private List<?> getCatList(long parentId) {
        // 创建查询条件
        TbItemCatExample example = new TbItemCatExample();
        TbItemCatExample.Criteria criteria = example.createCriteria();
        criteria.andParentIdEqualTo(parentId);
        List<TbItemCat> list = itemCatMapper.selectByExample(example);
        // 返回的数据集合
        List resultList = new ArrayList();
        int count = 0;// 一个计数器
        // 向resultList中添加节点
        for (TbItemCat tbItemCat : list) {
            // 判断是否为父节点
            if (tbItemCat.getIsParent()) {
                CatNode catNode = new CatNode();
                if (parentId == 0) {
                    catNode.setName("<a href='/products/"+tbItemCat.getId()+".html'>"+tbItemCat.getName()+"</a>");
                }  else {
                    catNode.setName(tbItemCat.getName());
                }
                catNode.setUrl("/products/"+tbItemCat.getId()+".html");
                catNode.setItem(getCatList(tbItemCat.getId()));
                resultList.add(catNode);
                count++;
                // 第一级显示14条记录
                if (parentId == 0 && count >= 14) {
                    break;
                }
            } else {
                // 如果是叶子节点
                resultList.add("/products/"+tbItemCat.getId()+".html|" + tbItemCat.getName());
            }
        }
        return resultList;
    }
}
