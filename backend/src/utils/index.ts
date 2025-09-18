import mongoose from "mongoose";
export const isValidMongoObjectId = (id) => {
  return mongoose.Types.ObjectId.isValid(id);
};
