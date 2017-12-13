package cn.medcn.user.service.impl;

import cn.medcn.common.Constants;
import cn.medcn.common.service.impl.BaseServiceImpl;
import cn.medcn.common.utils.StringUtils;
import cn.medcn.user.dao.CspUserPackageDAO;
import cn.medcn.user.dao.CspUserPackageDetailDAO;
import cn.medcn.user.model.CspPackage;
import cn.medcn.user.model.CspUserPackage;
import cn.medcn.user.model.CspUserPackageDetail;
import cn.medcn.user.service.CspUserPackageService;
import com.github.abel533.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by Liuchangling on 2017/12/8.
 */
@Service
public class CspUserPackageServiceImpl extends BaseServiceImpl<CspUserPackage> implements CspUserPackageService {
    @Autowired
    protected CspUserPackageDAO userPackageDAO;

    @Autowired
    protected CspUserPackageDetailDAO packageDetailDAO;

    @Override
    public Mapper<CspUserPackage> getBaseMapper() {
        return userPackageDAO;
    }

    /**
     * 判断是否有用户套餐信息
     * @param userId
     * @return
     */
    @Override
    public Boolean isNewUser(String userId) {
        CspUserPackage info = new CspUserPackage();
        info.setUserId(userId);
        Integer count = userPackageDAO.selectCount(info);
        return count > 0 ? false:true;
    }

    /**
     * 定时获取套餐过期的用户
     * @return
     */
    @Override
    public List<CspUserPackage> findCspUserPackageList() {
        return userPackageDAO.findUserPackages();
    }

    @Override
    public void addStanardInfo(String userId) {
        //用户添加标准套餐信息
        CspUserPackage userPackage = new CspUserPackage();
        userPackage.setUserId(userId);
        userPackage.setPackageId(CspPackage.TypeId.STANDARD.getId());
        userPackage.setUpdateTime(new Date());
        userPackage.setSourceType(Constants.NUMBER_ONE);
        userPackageDAO.insertSelective(userPackage);
    }


    /**
     * 定时获取套餐过期的用户
     * 将套餐降为标准版 同时将用户发布的会议加锁
     */
    @Override
    public void doModifyUserPackage(List<CspUserPackage> userPackageList) {
        Integer beforePackageId ;
        for (CspUserPackage userPackage : userPackageList) {
            // 变更之前的套餐id
            beforePackageId = userPackage.getPackageId();

            userPackage.setPackageId(CspPackage.TypeId.STANDARD.getId());
            userPackage.setPackageStart(null);
            userPackage.setPackageEnd(null);
            userPackage.setUpdateTime(new Date());
            userPackage.setSourceType(CspUserPackageDetail.modifyType.EXPIRE_DOWNGRADE.ordinal());
            userPackageDAO.updateByPrimaryKey(userPackage);

            // 记录用户套餐变更明细
            doAddUserPackageDetail(userPackage, beforePackageId);
        }
    }

    /**
     * 保存用户套餐变更明细
     * @param userPackage
     * @param beforePackageId
     */
    private void doAddUserPackageDetail(CspUserPackage userPackage, Integer beforePackageId) {
        CspUserPackageDetail detail = new CspUserPackageDetail();
        detail.setId(StringUtils.nowStr());
        detail.setUserId(userPackage.getUserId());
        detail.setBeforePackageId(beforePackageId);
        detail.setAfterPackageId(userPackage.getPackageId());
        detail.setUpdateTime(new Date());
        detail.setUpdateType(CspUserPackageDetail.modifyType.EXPIRE_DOWNGRADE.ordinal());
        packageDetailDAO.insert(detail);
    }


}
