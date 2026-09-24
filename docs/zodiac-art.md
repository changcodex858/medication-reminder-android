# 十二生肖卡面素材

使用内置 imagegen 生成一张 4 列 × 3 行图集，保留生成的透明通道，应用按单元格绘制，无外部图片服务或运行时下载。

资源：`app/src/main/res/drawable-nodpi/zodiac_atlas.png`

顺序：鼠、牛、虎、兔 / 龙、蛇、马、羊 / 猴、鸡、狗、猪。

生成提示词：

> Use case: stylized-concept. Create ONE production sprite atlas image for an Android medication reminder app. A precise 4 columns by 3 rows grid, equal square cells, 1536x1152 or same 4:3 aspect. Exactly twelve adorable premium 3D clay/ceramic Chinese zodiac animal figurines, one centered full body per cell. Row 1: rat, ox, tiger, rabbit. Row 2: Chinese dragon, snake, horse, goat. Row 3: monkey, rooster, dog, pig. Soft rounded forms, sophisticated warm materials, tiny friendly smiles, studio lighting, subtle contact shadows. Every animal has plenty of padding, no crossing cell boundaries. Each cell has the EXACT SAME solid very pale icy blue background #EDF5FA; no grid lines, no labels, no text, no numbers, no frames. Consistent camera perspective, scale and art direction throughout. Charming high end health companion product illustration, each animal immediately recognizable. Save generated local image for use as a single atlas asset.

生成结果为透明图集；背景渐变和高光由 Compose 绘制。卡片的立体感来自插画、阴影、渐变和按压倾斜，并非实时三维模型。
