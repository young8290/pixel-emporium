-- Keep existing category IDs and product foreign-key references stable while replacing only catalog content.
update category set name = '数码电子', icon = 'cat-digital.png', sort_order = 10 where id = 900001;
update category set name = '家用电器', icon = 'cat-appliance.png', sort_order = 20 where id = 900002;
update category set name = '服饰鞋包', icon = 'cat-apparel.png', sort_order = 30 where id = 900003;
update category set name = '家居家具', icon = 'cat-home.png', sort_order = 40 where id = 900004;
update category set name = '厨房餐厨', icon = 'cat-kitchen.png', sort_order = 50 where id = 900005;
update category set name = '美妆个护', icon = 'cat-beauty.png', sort_order = 60 where id = 900006;
update category set name = '图书文具', icon = 'cat-book.png', sort_order = 70 where id = 900007;
update category set name = '玩具乐器', icon = 'cat-toy.png', sort_order = 80 where id = 900008;
update category set name = '运动户外', icon = 'cat-sport.png', sort_order = 90 where id = 900009;
update category set name = '母婴用品', icon = 'cat-baby.png', sort_order = 100 where id = 900010;
update category set name = '宠物生活', icon = 'cat-pet.png', sort_order = 110 where id = 900011;
update category set name = '食品生鲜', icon = 'cat-grocery.png', sort_order = 120 where id = 900012;
update category set name = '健康保健', icon = 'cat-health.png', sort_order = 130 where id = 900013;
update category set name = '汽车用品', icon = 'cat-auto.png', sort_order = 140 where id = 900014;
update category set name = '手作艺术', icon = 'cat-craft.png', sort_order = 150 where id = 900015;
update category set name = '珠宝配饰', icon = 'cat-jewelry.png', sort_order = 160 where id = 900016;
update category set name = '文具办公', icon = 'cat-stationery.png', sort_order = 170 where id = 900017;
update category set name = '家装园艺', icon = 'cat-garden.png', sort_order = 180 where id = 900018;
update category set name = '其他', icon = 'cat-other.png', sort_order = 190 where id = 900019;

-- rollback:
-- restore the V6 names/icons for the same IDs; do not modify schema or product.category_id.
