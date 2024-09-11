package org.example.database;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.example.entity.User;
import org.example.entity.Service;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;

    static {
        try {
            Configuration configuration = new Configuration()
                    .configure() // loads hibernate.cfg.xml
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(Service.class);

            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
