SELECT 'Initializing database schemas...' AS progress;

CREATE DATABASE localkart_auth;
CREATE DATABASE localkart_user;
CREATE DATABASE localkart_product;
CREATE DATABASE localkart_inventory;
CREATE DATABASE localkart_order;
CREATE DATABASE localkart_payment;
CREATE DATABASE localkart_delivery;

SELECT 'Databases created successfully!' AS progress;
