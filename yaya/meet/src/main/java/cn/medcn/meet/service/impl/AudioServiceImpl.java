package cn.medcn.meet.service.impl;

import cn.medcn.common.excptions.NotEnoughCreditsException;
import cn.medcn.common.excptions.SystemException;
import cn.medcn.common.pagination.MyPage;
import cn.medcn.common.pagination.Pageable;
import cn.medcn.common.service.impl.BaseServiceImpl;
import cn.medcn.common.utils.CheckUtils;
import cn.medcn.common.utils.FileUtils;
import cn.medcn.goods.dto.CreditPayDTO;
import cn.medcn.goods.service.CreditsService;
import cn.medcn.meet.dao.*;
import cn.medcn.meet.dto.*;
import cn.medcn.meet.model.*;
import cn.medcn.meet.service.AudioService;
import cn.medcn.meet.service.LiveService;
import com.github.abel533.mapper.Mapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lixuan on 2017/4/25.
 */
@Service
public class AudioServiceImpl extends BaseServiceImpl<AudioCourse> implements AudioService {

    @Autowired
    private AudioCourseDetailDAO audioCourseDetailDAO;

    @Autowired
    private AudioCourseDAO audioCourseDAO;

    @Autowired
    private MeetAudioDAO meetAudioDAO;

    @Autowired
    private AudioHistoryDAO audioHistoryDAO;

    @Autowired
    private CreditsService creditsService;

    @Autowired
    protected LiveDetailDAO liveDetailDAO;


    @Value("${app.file.upload.base}")
    private String appFileUploadBase;

    @Autowired
    protected AudioCoursePlayDAO audioCoursePlayDAO;

    @Autowired
    protected LiveService liveService;

    @Override
    public Mapper<AudioCourse> getBaseMapper() {
        return audioCourseDAO;
    }


    /**
     * 批量生成ppt+语音信息
     *
     * @param list
     */
    @Override
    public void insertBatch(List<AudioCourseDetail> list) {
        for (AudioCourseDetail ppt:list){
            audioCourseDetailDAO.insert(ppt);
        }
    }

    @Override
    @Cacheable(value = DEFAULT_CACHE, key = "'audio_course_'+#courseId")
    public AudioCourse findAudioCourse(Integer courseId) {
        AudioCourse course = audioCourseDAO.selectByPrimaryKey(courseId);

        if (course != null) {
            List<AudioCourseDetail> details = audioCourseDetailDAO.findDetailsByCourseId(courseId);
            course.setDetails(details);
        }
        return course;
    }

    /**
     * 查询会议音频模块信息
     *
     * @param meetId
     * @param moduleId
     * @return
     */
    @Override
    public MeetAudio findMeetAudio(String meetId, Integer moduleId) {
        MeetAudio condition = new MeetAudio();
        condition.setMeetId(meetId);
        //condition.setModuleId(moduleId);
        MeetAudio meetAudio = meetAudioDAO.selectOne(condition);
        return meetAudio;
    }

    /**
     * 记录ppt语音学习记录
     *
     * @param history
     */
    @Override
    public void insertHistory(AudioHistory history) {
        AudioHistory condition = new AudioHistory();
        condition.setUserId(history.getUserId());
        condition.setDetailId(history.getDetailId());
        condition.setMeetId(history.getMeetId());
        AudioHistory existedHistory = audioHistoryDAO.selectOne(condition);
        if(existedHistory == null){
            history.setEndTime(new Date());
            history.setStartTime(new Date(history.getEndTime().getTime()- history.getUsedtime()*1000));
            audioHistoryDAO.insert(history);
        }else{
            existedHistory.setUsedtime(existedHistory.getUsedtime()+history.getUsedtime());
            existedHistory.setEndTime(new Date());
            existedHistory.setFinished(existedHistory.getFinished() || (history.getFinished()));
            audioHistoryDAO.updateByPrimaryKeySelective(existedHistory);
        }

    }

    /**
     * 查询资源列表
     *
     * @param pageable
     * @return
     */
    @Override
    public MyPage<CourseReprintDTO> findResource(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(), pageable.getPageSize(), true);
        MyPage<CourseReprintDTO> page = MyPage.page2Mypage((Page) audioCourseDAO.findResource(pageable.getParams()));
        return page;
    }

    /**
     * 查询所有的资源分类
     *
     * @return
     */
    @Override
    public List<ResourceCategoryDTO> findResourceCategorys(Integer userId) {
        return audioCourseDAO.findResourceCategorys(userId);
    }

    /**
     * 查询我的转载记录
     *
     * @param pageable
     * @return
     */
    @Override
    public MyPage<CourseReprintDTO> findMyReprints(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(), pageable.getPageSize(), Pageable.countPage);
        MyPage<CourseReprintDTO> page = MyPage.page2Mypage((Page) audioCourseDAO.findMyReprints(pageable.getParams()));
        return page;
    }

    /**
     * 查询我的分享记录
     *
     * @param pageable
     * @return
     */
    @Override
    public MyPage<CourseSharedDTO> findMyShared(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(), pageable.getPageSize(), Pageable.countPage);
        MyPage<CourseSharedDTO> page = MyPage.page2Mypage((Page) audioCourseDAO.findMyShared(pageable.getParams()));
        return page;
    }

    /**
     * 转载微课
     *
     * @param id
     * @param userId
     */
    @Override
    public void doReprint(Integer id, Integer userId) throws NotEnoughCreditsException,SystemException {
        AudioCourse course = audioCourseDAO.selectByPrimaryKey(id);
        if(course == null){
            throw new SystemException("您转载的会议[id="+id+"]不存在");
        }
        if(course.getShareType()!= null && course.getCredits() != null && course.getCredits() > 0){
            if(course.getShareType() == 1){
                //支付象数
                CreditPayDTO payDTO = new CreditPayDTO();
                payDTO.setAccepterDescrib("您的资源["+course.getTitle()+"]被转载,获得"+course.getCredits()+"个象数");
                payDTO.setPayerDescrib("您转载了资源["+course.getTitle()+"],消耗"+course.getCredits()+"个象数");
                payDTO.setPayer(userId);
                payDTO.setAccepter(course.getOwner());
                payDTO.setCredits(course.getCredits());
                creditsService.executePlayCredits(payDTO);
            }else if(course.getShareType() == 2){//奖励象数
                CreditPayDTO payDTO = new CreditPayDTO();
                payDTO.setCredits(course.getCredits());
                payDTO.setAccepter(userId);
                payDTO.setPayer(course.getOwner());
                payDTO.setPayerDescrib("您的资源["+course.getTitle()+"]被转载,支付"+course.getCredits()+"个象数");
                payDTO.setAccepterDescrib("您转载了资源["+course.getTitle()+"],获取"+course.getCredits()+"个象数");
                creditsService.executeAwardCredits(payDTO);
            }
        }
        doCopyCourse(course, userId, null);
    }

    /**
     * 复制微课信息
     * @param course
     * @param userId
     * @param newTitle
     */
    private Integer doCopyCourse(AudioCourse course, Integer userId, String newTitle){
        List<AudioCourseDetail> details = audioCourseDetailDAO.findDetailsByCourseId(course.getId());
        //复制微课信息
        AudioCourse reprintCourse = new AudioCourse();
        reprintCourse.setCredits(0);
        reprintCourse.setTitle(CheckUtils.isEmpty(newTitle) ? course.getTitle() : newTitle );
        reprintCourse.setPrimitiveId(course.getId());
        reprintCourse.setOwner(userId == null ? course.getOwner() : userId);
        reprintCourse.setCategory(course.getCategory());
        reprintCourse.setPublished(course.getPublished());
        reprintCourse.setShared(false);
        reprintCourse.setDeleted(false);
        reprintCourse.setCreateTime(new Date());
        reprintCourse.setSourceType(course.getSourceType());
        reprintCourse.setPlayType(course.getPlayType());
        reprintCourse.setCspUserId(course.getCspUserId());
        reprintCourse.setInfo(course.getInfo());
        audioCourseDAO.insert(reprintCourse);
        //复制微课明细
        for(AudioCourseDetail detail:details){
            AudioCourseDetail reprintDetail = new AudioCourseDetail();
            BeanUtils.copyProperties(detail, reprintDetail);
            reprintDetail.setId(null);
            reprintDetail.setCourseId(reprintCourse.getId());
            audioCourseDetailDAO.insert(reprintDetail);
        }
        return reprintCourse.getId();
    }


    /**
     * 查询我的被转载记录
     *
     * @param pageable
     * @return
     */
    @Override
    public MyPage<CourseReprintDTO> findMyReprinted(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(), pageable.getPageSize(), Pageable.countPage);
        MyPage<CourseReprintDTO> page = MyPage.page2Mypage((Page) audioCourseDAO.findMyReprinted(pageable.getParams()));
        return page;
    }

    /**
     * 修改会议ppt属性
     *
     * @param meetAudio
     */
    @Override
    public void updateMeetAudio(MeetAudio meetAudio) {
        meetAudioDAO.updateByPrimaryKeySelective(meetAudio);
    }

    /**
     * 获取meetAudio简单信息
     *
     * @param meetId
     * @param moduleId
     * @return
     */
    @Override
    public MeetAudio findMeetAudioSimple(String meetId, Integer moduleId) {
        MeetAudio condition = new MeetAudio();
        condition.setMeetId(meetId);
        //condition.setModuleId(moduleId);
        MeetAudio meetAudio = meetAudioDAO.selectOne(condition);
        return meetAudio;
    }

    /**
     * 根据id获取ppt明细记录
     *
     * @param detailId
     * @return
     */
    @Override
    public AudioCourseDetail findDetail(Integer detailId) {
        AudioCourseDetail detail = audioCourseDetailDAO.selectByPrimaryKey(detailId);
        return detail;
    }

    /**
     * 删除ppt明细
     * 同时维护本course的所有其他明细的下标
     *
     * @param detailId
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#courseId")
    public AudioCourseDetail deleteDetail(Integer courseId, Integer detailId) {
        AudioCourseDetail detail = audioCourseDetailDAO.selectByPrimaryKey(detailId);
        Integer startSort = detail.getSort();
        audioCourseDetailDAO.deleteByPrimaryKey(detailId);
        audioCourseDetailDAO.updateBatchDecreaseSort(detail.getCourseId(), startSort);
        //同时删除文件
        if(!StringUtils.isEmpty(detail.getAudioUrl())){
            FileUtils.deleteTargetFile(appFileUploadBase+detail.getAudioUrl());
        }
        if(!StringUtils.isEmpty(detail.getImgUrl())){
            FileUtils.deleteTargetFile(appFileUploadBase+detail.getImgUrl());
        }
        return detail;
    }

    /**
     * 修改ppt明细
     *
     * @param detail
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#detail.courseId")
    public void updateDetail(AudioCourseDetail detail) {
        audioCourseDetailDAO.updateByPrimaryKeySelective(detail);
    }

    /**
     * 增加ppt明细
     * 同时维护本course下的所有明细的下标
     *
     * @param detail
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#detail.courseId")
    public void addDetail(AudioCourseDetail detail) {
        Integer startSort = detail.getSort();
        audioCourseDetailDAO.updateBatchAddSort(detail.getCourseId(), startSort);
        audioCourseDetailDAO.insert(detail);
    }

    /**
     * 查询明细列表
     *
     * @param courseId
     * @return
     */
    @Override
    public List<AudioCourseDetail> findDetails(Integer courseId) {
        return audioCourseDetailDAO.findDetailsByCourseId(courseId);
    }

    /**
     * 查询ppt完整观看人数（全部、本月、本周）
     * @param params
     * @return
     */
    @Override
    public List<AudioHistoryDTO> findViewPptCount(Map<String,Object> params) {
        List<AudioHistoryDTO> list = audioCourseDetailDAO.findViewPptCount(params);
        for (AudioHistoryDTO audioHistoryDTO :list){
            audioHistoryDTO.setTagNo((Integer) params.get("tagNo"));
            audioHistoryDTO.setStartTime(params.get("startTime")==null?"":params.get("startTime").toString());
            audioHistoryDTO.setEndTime(params.get("endTime")==null?"":params.get("endTime").toString());
        }
        return list;
    }

    /**
     * 查询某个用户观看ppt时长明细
     * @param pageable
     * @return
     */
    @Override
    public MyPage<AudioRecordDTO> findAudioRecord(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(),pageable.getPageSize(), Pageable.countPage);
        MyPage<AudioRecordDTO> page = MyPage.page2Mypage((Page)audioCourseDetailDAO.findViewAudioList(pageable.getParams()));
        return page;
    }

    /**
     * 查询所有用户观看ppt时长明细
     * @param params
     * @return
     */
    @Override
    public List<AudioRecordDTO> findAudioRecordtoExcel(Map<String, Object> params) {
        List<AudioRecordDTO> list = audioCourseDetailDAO.findAudioRecordList(params);
        Map<Integer, Integer> map = new HashMap();
        Map<Integer,Integer> finishMap = new HashMap();
        for (AudioRecordDTO recordDTO : list){
            if(map.containsKey(recordDTO.getId())){//判断是否已经有该数值，如有，则累加观看时长
                map.put(recordDTO.getId(), map.get(recordDTO.getId()) + recordDTO.getUsedtime());
            }else{
                map.put(recordDTO.getId(), recordDTO.getUsedtime());
            }
            recordDTO.setTime(map.get(recordDTO.getId()));

            // 查询用户观看完的ppt记录
            params.put("id", recordDTO.getId());
            List<AudioRecordDTO> finishedPPTList = audioCourseDetailDAO.findFinishedPPtCount(params);
            if (finishedPPTList != null && finishedPPTList.size() != 0) {
                recordDTO.setPptCount(finishedPPTList.get(0).getPptCount());
            } else {
                recordDTO.setPptCount(0);
            }
        }

        return list;
    }



    /**
     * 查询ppt总页数
     * @param meetId
     * @return
     */
    public List<AudioCourseDetail> findPPtTotalCount(String meetId){
        List<AudioCourseDetail> list = audioCourseDetailDAO.findPPtTotalCount(meetId);
        return list;
    }

    /**
     * 查询观看ppt总人数
     * @param meetId
     * @return
     */
    @Override
    public Integer findViewCount(String meetId) {
        List list = audioCourseDetailDAO.findViewCount(meetId);
        Integer viewCount = 0;
        if(!CheckUtils.isEmpty(list)){
            viewCount = list.size();
        }
        return viewCount;
    }


    /**
     * 检测用户是否已经转载资源
     *
     * @param courseId
     * @param userId
     * @return
     */
    @Override
    public boolean checkReprinted(Integer courseId, Integer userId) {
        AudioCourse condition = new AudioCourse();
        condition.setOwner(userId);
        condition.setPrimitiveId(courseId);
        int count = audioCourseDAO.selectCount(condition);
        return count>0;
    }

    /**
     * 根据会议ID获取会议ppt模块信息
     *
     * @param meetId
     * @return
     */
    @Override
    public MeetAudio findMeetAudioByMeetId(String meetId) {
        MeetAudio condition = new MeetAudio();
        condition.setMeetId(meetId);
        return meetAudioDAO.selectOne(condition);
    }

    @Override
    public List<AudioRecordDTO> findFinishedPPtCount(Map<String,Object> params){
        List<AudioRecordDTO> recordList = audioCourseDetailDAO.findFinishedPPtCount(params);
        return recordList;
    }


    /**
     * 删除所有明细
     *
     * @param courseId
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#courseId")
    public void deleteAllDetails(Integer courseId) {
        AudioCourseDetail condition = new AudioCourseDetail();
        condition.setCourseId(courseId);
        audioCourseDetailDAO.delete(condition);
    }


    /**
     * 根据图片列表生成全新的微课明细
     *
     * @param courseId
     * @param pptImageList
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#courseId")
    public void updateAllDetails(Integer courseId, List<String> pptImageList) {
        deleteAllDetails(courseId);
        int sort = 1;
        for(String pptImage : pptImageList){
            AudioCourseDetail courseDetail = new AudioCourseDetail();
            courseDetail.setCourseId(courseId);
            courseDetail.setSort(sort);
            courseDetail.setImgUrl(pptImage);
            audioCourseDetailDAO.insert(courseDetail);
            sort++;
        }
    }

    /**
     * 查询用户观看某个会议的ppt页数
     * @param meetId
     * @param userId
     * @return
     */
    public Integer findUserViewPPTCount(String meetId,Integer userId){
        return audioHistoryDAO.findUserViewPPTCount(meetId,userId);
    }

    public void addMeetAudio(MeetAudio audio){
        meetAudioDAO.insert(audio);
    }

    /**
     * 查询csp会议列表
     * @return
     */
    public MyPage<CourseDeliveryDTO> findCspMeetingList(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(), pageable.getPageSize(), true);
        PageHelper.orderBy("c.create_time " + (pageable.get("sortType") == null ? "desc" : pageable.get("sortType")));
        return MyPage.page2Mypage((Page) audioCourseDAO.findCspMeetingList(pageable.getParams()));
    }

    @Override
    public AudioCourse findLastDraft(String cspUserId) {
        AudioCourse course = audioCourseDAO.findLastDraft(cspUserId);
        if (course != null) {
            List<AudioCourseDetail> details = audioCourseDetailDAO.findDetailsByCourseId(course.getId());
            course.setDetails(details);
        }
        return course;
    }

    @Override
    public AudioCoursePlay findPlayState(Integer courseId) {
        AudioCoursePlay cond = new AudioCoursePlay();
        cond.setCourseId(courseId);
        return audioCoursePlayDAO.selectOne(cond);
    }

    /**
     * 修改录播记录
     *
     * @param play
     */
    @Override
    public void updateAudioCoursePlay(AudioCoursePlay play) {
        audioCoursePlayDAO.updateByPrimaryKey(play);
    }

    /**
     * 投稿给指定用户的会议列表
     * @param pageable
     * @return
     */
    @Override
    public MyPage<CourseDeliveryDTO> findHistoryDeliveryByAcceptId(Pageable pageable) {
        PageHelper.startPage(pageable.getPageNum(), pageable.getPageSize(), true);
        return MyPage.page2Mypage((Page)audioCourseDAO.findHistoryDeliveryByAcceptId(pageable.getParams()));
    }

    /**
     * 删除course以及明细
     *
     * @param courseId
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#courseId")
    public void deleteAudioCourse(Integer courseId) {
        deleteAllDetails(courseId);
        audioCourseDAO.deleteByPrimaryKey(courseId);
    }

    /**
     * 复制课件 并复制直播和录播信息
     *
     * @param courseId
     */
    @Override
    public int addCourseCopy(Integer courseId, String newTitle) {
        AudioCourse course = audioCourseDAO.selectByPrimaryKey(courseId);
        Integer newCourseId = doCopyCourse(course, null, newTitle);

        Live live = liveService.findByCourseId(courseId);
        if (live != null) {
            Live copy = new Live();
            BeanUtils.copyProperties(live, copy);
            copy.setId(cn.medcn.common.utils.StringUtils.nowStr());
            copy.setReplayUrl(null);
            copy.setLiveState(AudioCoursePlay.PlayState.init.ordinal());
            copy.setLivePage(0);
            copy.setHdlUrl(null);
            copy.setHlsUrl(null);
            copy.setRtmpUrl(null);
            copy.setPlayCount(0);
            copy.setCourseId(newCourseId);
            liveService.insert(copy);
        }

        AudioCoursePlay cond = new AudioCoursePlay();
        cond.setCourseId(courseId);
        AudioCoursePlay play = audioCoursePlayDAO.selectOne(cond);
        if (play != null) {
            AudioCoursePlay copy = new AudioCoursePlay();
            BeanUtils.copyProperties(play, copy);
            copy.setId(cn.medcn.common.utils.StringUtils.nowStr());
            copy.setPlayState(AudioCoursePlay.PlayState.init.ordinal());
            copy.setPlayPage(0);
            copy.setCourseId(newCourseId);
            audioCoursePlayDAO.insert(copy);
        }

        return newCourseId;
    }

    /**
     * 修改课件基本信息以及直播信息
     *
     * @param audioCourse
     * @param live
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#audioCourse.id")
    public void updateAudioCourseInfo(AudioCourse audioCourse, Live live) {
        Live oldLive = liveService.findByCourseId(audioCourse.getId());

        if (oldLive == null) {
            oldLive = new Live();
            oldLive.setId(cn.medcn.common.utils.StringUtils.nowStr());
            oldLive.setCourseId(audioCourse.getId());
            oldLive.setLiveState(Live.LiveState.init.getType());
            oldLive.setLivePage(0);
            oldLive.setVideoLive(audioCourse.getPlayType().intValue() == AudioCourse.PlayType.live_video.getType());
            oldLive.setStartTime(live.getStartTime());
            oldLive.setEndTime(live.getEndTime());

            liveService.insert(oldLive);
        } else {
            oldLive.setVideoLive(audioCourse.getPlayType() != null && audioCourse.getPlayType().intValue() == AudioCourse.PlayType.live_video.getType());
            oldLive.setStartTime(live.getStartTime());
            oldLive.setEndTime(live.getEndTime());

            liveService.updateByPrimaryKeySelective(oldLive);
        }
        audioCourse.setDeleted(false);
        audioCourse.setPublished(true);
        updateByPrimaryKeySelective(audioCourse);
    }


    /**
     * 修改课件基本信息以及修改录播信息
     *
     * @param audioCourse
     * @param play
     */
    @Override
    @CacheEvict(value = DEFAULT_CACHE, key = "'audio_course_'+#audioCourse.id")
    public void updateAudioCourseInfo(AudioCourse audioCourse, AudioCoursePlay play) {
        AudioCoursePlay oldPlay = findPlayState(audioCourse.getId());

        if (oldPlay == null) {
            oldPlay = new AudioCoursePlay();
            oldPlay.setId(cn.medcn.common.utils.StringUtils.nowStr());
            oldPlay.setCourseId(audioCourse.getId());
            oldPlay.setPlayPage(0);
            oldPlay.setPlayState(AudioCoursePlay.PlayState.init.ordinal());

            audioCoursePlayDAO.insert(oldPlay);
        }
        audioCourse.setDeleted(false);
        audioCourse.setPlayType(AudioCourse.PlayType.normal.getType());
        audioCourse.setPublished(true);
        updateByPrimaryKeySelective(audioCourse);
    }

    @Override
    public void insertAudioCoursePlay(AudioCoursePlay play) {
        audioCoursePlayDAO.insert(play);
    }

    @Override
    @CacheEvict(value = DEFAULT_CACHE,  key = "'audio_course_'+#liveDetail.courseId")
    public void addLiveDetail(LiveDetail liveDetail) {
        liveDetailDAO.insert(liveDetail);
    }

    @Override
    public Integer findMaxLiveDetailSort(Integer courseId) {
        return liveDetailDAO.findMaxLiveDetailSort(courseId);
    }

    @Override
    public List<AudioCourseDetail> findLiveDetails(Integer courseId) {
        List<AudioCourseDetail> details = liveDetailDAO.findByCourseId(courseId);
        LiveOrderDTO orderDTO = liveService.findCachedOrder(courseId);
        if (details.size() == 0) {//直播明细为空时
            if (orderDTO == null) {//如果缓存中也没有则加入第一张
                AudioCourseDetail cond = new AudioCourseDetail();
                cond.setCourseId(courseId);
                cond.setSort(1);

                AudioCourseDetail firstDetail = audioCourseDetailDAO.selectOne(cond);
                details.add(firstDetail);
            }
        }
        if (orderDTO != null) {
            AudioCourseDetail detail = new AudioCourseDetail();
            detail.setId(orderDTO.getDetailId());
            detail.setCourseId(Integer.valueOf(orderDTO.getCourseId()));
            detail.setImgUrl(orderDTO.getImgUrl());
            detail.setAudioUrl(orderDTO.getAudioUrl());
            detail.setVideoUrl(orderDTO.getVideoUrl());
            details.add(detail);
        }

        return details;
    }
}
