package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.mapper.UserMapper;
import com.yupi.springbootinit.model.entity.User;
import generator.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author zc
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2023-07-27 15:32:05
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




