package cn.medcn.meet.service;

import cn.medcn.common.pagination.MyPage;
import cn.medcn.common.pagination.Pageable;
import cn.medcn.common.service.BaseService;
import cn.medcn.meet.dto.CourseThemeDTO;
import cn.medcn.meet.model.AudioCourseTheme;
import cn.medcn.meet.model.BackgroundImage;
import cn.medcn.meet.model.BackgroundMusic;

import java.util.List;

/**
 * Created by Liuchangling on 2018/1/19.
 * 课程主题
 */
public interface CourseThemeService extends BaseService<AudioCourseTheme> {

    CourseThemeDTO findCourseTheme(Integer courseId);

    List<BackgroundImage> findImageList();

    List<BackgroundMusic> findMusicList();

    MyPage<BackgroundImage> findImagePageList(Pageable pageable);

    MyPage<BackgroundMusic> findMusicPageList(Pageable pageable);


    void addBackgroundImage(BackgroundImage image);

    void addBackgroundMusic(BackgroundMusic music);
}
