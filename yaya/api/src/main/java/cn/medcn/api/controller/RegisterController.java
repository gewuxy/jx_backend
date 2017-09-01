package cn.medcn.api.controller;

import cn.medcn.common.Constants;
import cn.medcn.common.ctrl.BaseController;
import cn.medcn.common.email.EmailHelper;
import cn.medcn.common.service.BaiduApiService;
import cn.medcn.common.service.JSmsService;
import cn.medcn.common.supports.baidu.NearbySearchDTO;
import cn.medcn.common.supports.baidu.SearchResultDTO;
import cn.medcn.common.utils.APIUtils;
import cn.medcn.common.utils.MD5Utils;
import cn.medcn.common.utils.RedisCacheUtils;
import cn.medcn.common.utils.RegexUtils;
import cn.medcn.sys.model.SystemProperties;
import cn.medcn.sys.model.SystemRegion;
import cn.medcn.sys.service.SysPropertiesService;
import cn.medcn.sys.service.SystemRegionService;
import cn.medcn.user.dto.AppUserDTO;
import cn.medcn.user.dto.Captcha;
import cn.medcn.user.dto.HospitalDTO;
import cn.medcn.user.dto.TitleDTO;
import cn.medcn.user.model.ActiveStore;
import cn.medcn.user.model.AppRole;
import cn.medcn.user.model.AppUser;
import cn.medcn.user.model.Department;
import cn.medcn.user.service.AppUserService;
import cn.medcn.user.service.HospitalService;
import com.google.common.collect.Lists;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by lixuan on 2017/4/20.
 */
@RequestMapping(value="/api/register")
@Controller
public class RegisterController extends BaseController{

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private SystemRegionService systemRegionService;

    @Autowired
    private EmailHelper emailHelper;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private JSmsService jSmsService;

    @Autowired
    private BaiduApiService baiduApiService;

    @Autowired
    private SysPropertiesService sysPropertiesService;


    @Value("${app.yaya.base}")
    private String appBaseUrl;

    @Value("${app.file.base}")
    private String appFileBaseUrl;


    /**
     * 生成短信验证码给前端
     * @param mobile
     * @param type  type=0或者空时表示注册时或者绑定新手机号时获取验证码,1表示重置密码时获取验证码
     * @return
     */
    @RequestMapping("/get_captcha")
    @ResponseBody
    public String getCode(String mobile,Integer type){

        if(!RegexUtils.checkMobile(mobile)){
            return error("手机格式不正确");
        }

        AppUser user = new AppUser();
        user.setMobile(mobile);


        //10分钟内最多允许获取3次验证码
        Captcha captcha = (Captcha)redisCacheUtils.getCacheObject(mobile);
        if(captcha == null){ //第一次获取
            String msgId = null;
            try {
                msgId = jSmsService.send(mobile, Constants.DEFAULT_TEMPLATE_ID);
            } catch (Exception e) {
                return error("发送短信失败");
            }
            Captcha firstCaptcha = new Captcha();
            firstCaptcha.setFirstTime(new Date());
            firstCaptcha.setCount(Constants.NUMBER_ZERO);
            firstCaptcha.setMsgId(msgId);
            redisCacheUtils.setCacheObject(mobile,firstCaptcha,Constants.CAPTCHA_CACHE_EXPIRE_TIME); //15分钟有效期
            return success();

        }else {
            Long between = System.currentTimeMillis() - captcha.getFirstTime().getTime();
            if(captcha.getCount() == 2 && between < TimeUnit.MINUTES.toMillis(10)){
                return error("获取验证码次数频繁，请稍后");
            }
            String msgId = null;
            try {
                msgId = jSmsService.send(mobile, Constants.DEFAULT_TEMPLATE_ID);
            } catch (Exception e) {
                return APIUtils.error("发送短信失败");
            }
            captcha.setMsgId(msgId);
            captcha.setCount(captcha.getCount() + 1);
            redisCacheUtils.setCacheObject(mobile,captcha,Constants.CAPTCHA_CACHE_EXPIRE_TIME);
        }

        return success();
    }


    /**
     * 获取医院级别
     * @return
     */
    @RequestMapping("/properties")
    @ResponseBody
    public String getProperties(Integer version){
        SystemProperties properties = new SystemProperties();
        List<SystemProperties> list = null;
        list = sysPropertiesService.select(properties);
        Integer newVersion = getNewVersion(list);
        Map<String,Object> map = new HashedMap();
        map.put("version",newVersion);
        if(version != null && version >= newVersion){  //版本无变化
            return success(map);
        }
        //第一次获取或者版本有变化

        for(SystemProperties properties1:list){
            if(!StringUtils.isEmpty(properties1.getPicture())){
                properties1.setPicture(appFileBaseUrl + properties1.getPicture());
            }
        }
        map.put("propList",list);
        return success(map);

    }

    private Integer getNewVersion(List<SystemProperties> list){
        Integer version = list.get(0).getVersion();
        for(int i=1;i<list.size();i++){
            if(list.get(i).getVersion() > version){
                version = list.get(i).getVersion();
            }
        }
        return version;
    }

    /**
     * 获取职称
     * @return
     */
    @RequestMapping("/title")
    @ResponseBody
    public String getTitleList(){
        List<TitleDTO> dtoList = new ArrayList<>();
        List<String> gradeList = new ArrayList<>();
        gradeList.add("医师");
        gradeList.add("药师");
        gradeList.add("护师");
        gradeList.add("技师");

        TitleDTO dto1 = new TitleDTO();
        dto1.setTitle("高级职称");
        dto1.setGrade(gradeList);

        TitleDTO dto2 = new TitleDTO();
        dto2.setTitle("中级职称");
        dto2.setGrade(gradeList);

        TitleDTO dto3 = new TitleDTO();
        dto3.setTitle("初级职称");
        dto3.setGrade(gradeList);

        TitleDTO dto4 = new TitleDTO();
        dto4.setTitle("其他");
        List<String> list = new ArrayList<>();
        list.add("其他职称");
        dto4.setGrade(list);

        dtoList.add(dto1);
        dtoList.add(dto2);
        dtoList.add(dto3);
        dtoList.add(dto4);
        return success(dtoList);
    }


    /**
     * 获取专科列表
     * @return
     */
    @RequestMapping("/specialty")
    @ResponseBody
    public String findSpecialtyList(){
        List<Department> list = hospitalService.findAllDepart();
        return success(list);
    }

    @RequestMapping("/specialties")
    @ResponseBody
    public String specialty(){
        List<Department> list = hospitalService.findAllDepart();
        Map<String,List<String>> map = new HashMap<>();
        for(Department department:list){
            List<String> nameList = new ArrayList<>();
            map.put(department.getCategory(),nameList);
        }
        Set<String> set = map.keySet();
        for(String category:set){
            for(Department department:list){
                if(category.equals(department.getCategory())){
                    map.get(category).add(department.getName());
                }
            }
        }
        return success(map);
    }


    /**
     * app扫码注册检查提供方是否有邀请码
     * @param masterId
     * @return
     */
    @RequestMapping("/scan_register")
    @ResponseBody
    public String checkInvite(Integer[] masterId){
        if(masterId == null){ //没有激活码提供方
            return success();
        }
        Map<String,List<Object>> map = new HashedMap();
        map.put("masterId",new ArrayList());
        map.put("name",new ArrayList());
        Boolean flag = false;
        for(Integer id:masterId){
            ActiveStore store = appUserService.getActiveStore(id);
            if(store != null && store.getStore() > 0){
                map.get("masterId").add(id);
                AppUser user = appUserService.selectByPrimaryKey(id);
                map.get("name").add(user == null? "" : user.getLinkman());
                flag = true;
            }
        }
        if(flag){
            return success(map);
        }else {
            return error("激活码数量为0");
        }
    }

    /**
     *     用户app注册
     * @param dto
     * @param invite 邀请码,非扫描邀请码注册需提供
     * @param captcha 验证码
     * @param masterId 邀请码提供方id，扫描邀请码二维码时需提供
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/reg",method = RequestMethod.POST)
    @ResponseBody
    public String register(AppUserDTO dto, String invite,String captcha,Integer[] masterId)  {

        String data = checkData(dto,captcha,invite,masterId);
        if( data != null){
            return data;
        }

        //设置医院级别
        SystemProperties properties = new SystemProperties();
        properties.setId(dto.getHospitalLevel());
        properties = sysPropertiesService.selectOne(properties);
        if(properties != null){
            dto.setHosLevel(properties.getPropValue());
        }else {
            return error("医院级别不正确");
        }
        AppUser searchUser = new AppUser();
        searchUser.setMobile(dto.getMobile());
        if(appUserService.selectOne(searchUser) != null){
            return APIUtils.error(APIUtils.USER_EXIST_CODE,"该用户已存在");
        }

        AppUser user = AppUserDTO.rebuildToDoctor(dto);

        user.setRoleId(AppRole.AppRoleType.DOCTOR.getId());
        if(StringUtils.isEmpty(user.getNickname())){
            user.setNickname(user.getLinkman());
        }
        user.setRegistDate(new Date());
        user.setPubFlag(false);

        Captcha captcha1 = (Captcha)redisCacheUtils.getCacheObject(dto.getMobile());
        try {
            if(!jSmsService.verify(captcha1.getMsgId(),captcha)){
                return error("验证码不正确");
            }
        } catch (Exception e) {
            return error("验证码已被校验，请重新获取验证码");
        }
        try{
            //检查是否是测试用邀请码,测试邀请码返回false
            if(hadCheckInvite(dto.getHospital(), invite)){
                //已检查invite,masterId，不可能同时为空
                appUserService.executeRegist(user, invite,masterId);//真实用户注册
            }else{
                appUserService.executeRegist(user, null,null);  //测试用户注册
            }
        }catch (Exception e){
            return error(e.getMessage());
        }
        return success();
    }



    private String checkData(AppUserDTO dto,String captcha,String invite,Integer[] masterId) {
        if(StringUtils.isEmpty(dto.getMobile())){
            return error("手机号码不能为空");
        }
        if(!RegexUtils.checkMobile(dto.getMobile())){
            return error("手机格式不正确");
        }

        if(StringUtils.isEmpty(captcha)){
            return error("验证码不能为空");
        }

        if(StringUtils.isEmpty(dto.getPassword())){
            return error("密码不能为空");
        }
        if(StringUtils.isEmpty(dto.getLinkman())){
            return error("真实姓名不能为空");
        }

        if(StringUtils.isEmpty(dto.getProvince())){
            return error("省份不能为空");
        }

        if(StringUtils.isEmpty(dto.getCity())){
            return error("城市不能为空");
        }

        if(StringUtils.isEmpty(dto.getHospital())){
            return error("医院不能为空");
        }
        if(StringUtils.isEmpty(dto.getCategory()) || StringUtils.isEmpty(dto.getName())){
            return error("专科名称不能为空");
        }

        if(StringUtils.isEmpty(dto.getTitle())){
            return error("职称不能为空");
        }

        //非扫描邀请码注册需要填写邀请码
        if(masterId == null && StringUtils.isEmpty(invite)){
            return error("邀请码不能为空");
        }
        return null;
    }


    /**
     * 检查是否是测试用邀请码,测试邀请码返回false
     * @param hosName
     * @param invite
     * @return
     */
    private boolean hadCheckInvite(String hosName, String invite){

        if(Constants.DEFAULT_HOS_NAME.equals(hosName) && Constants.DEFAULT_INVITE.equals(invite)){
            return false;
        }
        return true;
    }



    @RequestMapping(value="/provinces")
    @ResponseBody
    public String provinces(){
        List<SystemRegion> list = systemRegionService.findRegionByPreid(0);
        return success(list);
    }


    @RequestMapping(value="/cities")
    @ResponseBody
    public String cities(Integer preId){
        if(preId == null){
            return error("省份ID不能为空");
        }
        List<SystemRegion> list = systemRegionService.findRegionByPreid(preId);
        return success(list);
    }


    @RequestMapping(value="/zone")
    @ResponseBody
    public String zones(Integer preId){
        if(preId == null){
            return error("城市ID不能为空");
        }
        List<SystemRegion> list = systemRegionService.findRegionByPreid(preId);
        return success(list);
    }




    @RequestMapping(value="/hos")
    @ResponseBody
    public String hos(String city){
        if(StringUtils.isEmpty(city)){
            return error("城市信息不能为空");
        }
        List<String> list = hospitalService.findHospitals(city);
        return success(HospitalDTO.buildList(list));
    }


    /**
     * 通过手机重置密码
     * @param mobile
     * @param captcha
     * @param password
     * @return
     */
    @RequestMapping("/pwd/reset/by_mobile")
    @ResponseBody
    public String pwdResetByMobile(String mobile,String captcha,String password ){
        if(StringUtils.isEmpty(password)){
            return error("密码不能为空");
        }
        if(StringUtils.isEmpty(mobile)){
            return error("手机号码不能为空");
        }
        if(!RegexUtils.checkMobile(mobile)){
            return error("手机格式不正确");
        }
        Captcha captcha1 = (Captcha)redisCacheUtils.getCacheObject(mobile);
        try {
            if(!jSmsService.verify(captcha1.getMsgId(),captcha)){
                return error("验证码不正确");
            }
        } catch (Exception e) {
            return error("验证码不正确");
        }

        AppUser condition = new AppUser();
        condition.setMobile(mobile);
        AppUser user = appUserService.selectOne(condition);
        if (user == null) {
            return error("用户不存在");
        }
        user.setPassword(MD5Utils.MD5Encode(password));
        appUserService.updateByPrimaryKeySelective(user);

        return success();

    }




    @RequestMapping("/get_invite_code")
    public String getInviteCode(){
        return "/register/getInviteCode";
    }

    @RequestMapping("/get_protocol")
    public String getProtocol(){
        String url = "/view/article/17051509491821468946";
        return "redirect:" + url;
    }

    /**
     * 获取附近医院
     * @param searchDTO
     * @return
     */
    @RequestMapping(value = "/nearby/hospital")
    @ResponseBody
    public String nearbyHospital(NearbySearchDTO searchDTO){
        searchDTO.setQuery(NearbySearchDTO.QUERY_HOSPITAL+","+searchDTO.getQuery());
        SearchResultDTO searchResult = baiduApiService.search(searchDTO);
        return success(searchResult);
    }

    @RequestMapping(value = "/regions")
    @ResponseBody
    public String allRegion(){
        List<SystemRegion> regions = systemRegionService.findAll();
        List<SystemRegion> result = Lists.newArrayList();
        for(SystemRegion region : regions){
            if (region.getLevel() == 1){
                setSubList(region, regions);
                result.add(region);
            }
        }

        return success(result);
    }

    private void setSubList(SystemRegion region, List<SystemRegion> regions){
        for(SystemRegion sub:regions){
            if(region.getId().intValue() == sub.getPreId().intValue()){
                if(region.getLevel() < 3){
                    setSubList(sub, regions);
                }
                region.getDetails().add(sub);
            }
        }
    }

}
