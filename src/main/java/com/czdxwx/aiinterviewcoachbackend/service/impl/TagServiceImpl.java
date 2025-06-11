package com.czdxwx.aiinterviewcoachbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.czdxwx.aiinterviewcoachbackend.entity.Tag;
import com.czdxwx.aiinterviewcoachbackend.mapper.postgres.TagMapper;
import com.czdxwx.aiinterviewcoachbackend.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author 12265
* @description 针对表【tags】的数据库操作Service实现
* @createDate 2025-06-10 19:32:58
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService {

}




