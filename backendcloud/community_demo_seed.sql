insert into community_post (user_id, title, content, tag, likes, createtime)
select 1, 'Alpha-Beta Bot 的防守优化记录',
       '最近在测试 Bot 的时候发现，只判断当前一步很容易漏掉对手的连续威胁。我尝试加入两层 Alpha-Beta 搜索之后，Bot 对活三和冲四的处理明显稳定了很多。这里想和大家讨论一下，五子棋 Bot 的防守权重应该放在多高比较合适。',
       'Bot策略', 0, now() - interval 7 day
where not exists (select 1 from community_post where title = 'Alpha-Beta Bot 的防守优化记录');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 2, '有没有适合新手的五子棋开局思路',
       '我发现自己下棋时经常开局很随意，后面就会被对手牵着走。想问问大家，黑棋先手时优先占中心之后，第二步和第三步有没有比较稳的选择？如果写进 Bot 的开局库，应该保存多少种定式比较合适？',
       '五子棋技巧', 0, now() - interval 6 day
where not exists (select 1 from community_post where title = '有没有适合新手的五子棋开局思路');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 1, 'Bot 测评报告里的效率分怎么提升',
       '我的 Bot 胜率还可以，但是效率分一直很低。看回放发现它经常在没有明显威胁的时候绕远路，导致平均步数偏多。我准备给候选点加一个距离中心和距离已有棋子的权重，不知道这样会不会让搜索更快。',
       '代码问题', 0, now() - interval 5 day
where not exists (select 1 from community_post where title = 'Bot 测评报告里的效率分怎么提升');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 2, '一次被斜向五连偷家的复盘',
       '今天对局里我一直盯着横向和纵向，结果忽略了右上到左下的斜线。对手连续三步把斜向做成活四，我才发现已经来不及堵了。建议大家写 Bot 的时候一定要把四个方向的威胁评估统一处理。',
       '对局复盘', 0, now() - interval 4 day
where not exists (select 1 from community_post where title = '一次被斜向五连偷家的复盘');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 1, '社区搜索现在支持 Elasticsearch 了',
       '刚试了一下社区搜索，可以直接搜标题、正文和作者，命中的关键词会高亮。这个功能对找历史 Bot 优化经验挺有用，后面如果帖子多了，还可以做热门讨论和相关推荐。',
       '综合讨论', 0, now() - interval 3 day
where not exists (select 1 from community_post where title = '社区搜索现在支持 Elasticsearch 了');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 2, '关于活三、冲四、活四的评分建议',
       '我目前的评分是活四最高，冲四其次，活三再往下。但是遇到双活三的时候，实际威胁可能比单个冲四更大。大家觉得评估函数里要不要专门识别组合威胁？',
       'Bot策略', 0, now() - interval 2 day
where not exists (select 1 from community_post where title = '关于活三、冲四、活四的评分建议');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 1, 'DeepSeek 赛后分析给我的 Bot 改进建议',
       '我让 DeepSeek 分析了最近一次 Bot 测评，它指出我的主要问题是没有搜索深度，只靠一步贪心判断。它建议先做候选点裁剪，再用 Alpha-Beta 搜索两层，这样不会太慢，也能提前发现对手威胁。',
       'Bot策略', 0, now() - interval 1 day
where not exists (select 1 from community_post where title = 'DeepSeek 赛后分析给我的 Bot 改进建议');

insert into community_post (user_id, title, content, tag, likes, createtime)
select 2, '匹配功能和 Bot 测试模块可以怎么演示',
       '准备给老师演示的时候，我打算先用真人匹配展示 WebSocket 对战，再用 Bot 测试模块展示内置 Bot 测评，最后打开社区搜索历史讨论。这样微服务、队列、Redis、ES 都能串起来。',
       '综合讨论', 0, now()
where not exists (select 1 from community_post where title = '匹配功能和 Bot 测试模块可以怎么演示');

insert ignore into community_post_like (post_id, user_id, createtime)
select id, 2, now() - interval 2 day from community_post
where title in ('Alpha-Beta Bot 的防守优化记录', 'Bot 测评报告里的效率分怎么提升', '社区搜索现在支持 Elasticsearch 了', 'DeepSeek 赛后分析给我的 Bot 改进建议');

insert ignore into community_post_like (post_id, user_id, createtime)
select id, 1, now() - interval 1 day from community_post
where title in ('有没有适合新手的五子棋开局思路', '一次被斜向五连偷家的复盘', '关于活三、冲四、活四的评分建议', '匹配功能和 Bot 测试模块可以怎么演示');

insert into community_comment (post_id, user_id, content, createtime)
select p.id, 2, '这个思路挺清楚的，我也觉得防守权重不能只看当前是否能堵住，还要看下一步会不会继续被追杀。', now() - interval 6 day
from community_post p
where p.title = 'Alpha-Beta Bot 的防守优化记录'
  and not exists (select 1 from community_comment c where c.post_id = p.id and c.content = '这个思路挺清楚的，我也觉得防守权重不能只看当前是否能堵住，还要看下一步会不会继续被追杀。');

insert into community_comment (post_id, user_id, content, createtime)
select p.id, 1, '可以先固定中心附近的几个开局点，等中盘再交给评估函数，不然开局库太大也不好维护。', now() - interval 5 day
from community_post p
where p.title = '有没有适合新手的五子棋开局思路'
  and not exists (select 1 from community_comment c where c.post_id = p.id and c.content = '可以先固定中心附近的几个开局点，等中盘再交给评估函数，不然开局库太大也不好维护。');

insert into community_comment (post_id, user_id, content, createtime)
select p.id, 2, '距离已有棋子的权重很有用，可以先只搜索周围两格内的空点，效率会高很多。', now() - interval 4 day
from community_post p
where p.title = 'Bot 测评报告里的效率分怎么提升'
  and not exists (select 1 from community_comment c where c.post_id = p.id and c.content = '距离已有棋子的权重很有用，可以先只搜索周围两格内的空点，效率会高很多。');

insert into community_comment (post_id, user_id, content, createtime)
select p.id, 1, '双活三确实要单独加分，不然 Bot 会低估这种一手两威胁的局面。', now() - interval 1 day
from community_post p
where p.title = '关于活三、冲四、活四的评分建议'
  and not exists (select 1 from community_comment c where c.post_id = p.id and c.content = '双活三确实要单独加分，不然 Bot 会低估这种一手两威胁的局面。');

update community_post p
set likes = (
    select count(*)
    from community_post_like l
    where l.post_id = p.id
);
