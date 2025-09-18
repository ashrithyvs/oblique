import jwt, { Secret } from "jsonwebtoken";
import { Response, NextFunction } from "express";
import { JWT_SECRET } from "../config";

const ACCESS_TOKEN_SECRET: Secret = JWT_SECRET;


export const authTokenValidation = async (
  req: any,
  res: Response,
  next: NextFunction,
) => {
  if (req.headers?.tenantid || (req.headers?.raceid === 'race-inboound' && req.headers?.raceconfig === 'x') ) {
    next();
  } else if (req.headers?.authorization) {
    const token = req.headers.authorization.split(" ")[1];
    if (token) {
      jwt.verify(token, ACCESS_TOKEN_SECRET, (err, decode:any) => {
        if (err) {
          res.status(401).send({ message: "Missing Token" });
        } else {
          const exp = decode["exp"] * 1000;
          if (new Date(exp) < new Date()) {
            res.status(401).send({ message: "Token Expired" });
          } else {
            // console.log("decode",decode)
            req.authInfo = {...decode};
            req.body={...req.body,createdBy:decode.userId}
            next("route");
          }
        }
      });
    } else {
      res.status(401).send({ message: "Missing Token" });
    }
  } else {
    res.status(401).send({ message: "Missing Token" });
  }
};
