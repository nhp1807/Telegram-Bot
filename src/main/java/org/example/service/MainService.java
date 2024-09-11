package org.example.service;

import org.example.database.HibernateUtil;
import org.example.entity.Service;
import org.example.entity.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.HashSet;
import java.util.Set;

public class MainService {
    private static final SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
    private static final Session session = sessionFactory.openSession();

    public void createServiceWithUsers() {
//        // Create a new user and service
//        User user = new User();
//        user.setUsername("user1");
//
//        Service service = new Service();
//        service.setServiceName("service1");
//
//        // Associate user with service
//        user.getServices().add(service);
//        service.getUsers().add(user);
//
//        // Save to database
//        Session session = HibernateUtil.getSessionFactory().openSession();
//        Transaction transaction = session.beginTransaction();
//
//        session.save(user);
//        session.save(service);
//
//        transaction.commit();
//        session.close();
    }
}

